import { useState, useRef, useEffect } from "react"
import { sendChatMessage } from "../services/api"
import { useAuth } from "../context/AuthContext"

export default function ChatPage() {
    const { user } = useAuth()
    const [messages, setMessages] = useState([
        { role: "model", content: "Olá! Sou o teu AI Stylist pessoal. Que look procuras hoje ou que peça gostavas de adicionar ao armário?" }
    ])
    const [input, setInput] = useState("")
    const [loading, setLoading] = useState(false)
    const messagesEndRef = useRef(null)

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
    }

    useEffect(() => {
        scrollToBottom()
    }, [messages])

    const handleSend = async (e) => {
        e.preventDefault()
        if (!input.trim() || loading) return

        const userMsg = { role: "user", content: input }
        setMessages(prev => [...prev, userMsg])
        setInput("")
        setLoading(true)

        try {
            const history = messages.filter(m => m.role !== "model" || !m.content.startsWith("Olá! Sou o teu AI Stylist"))
            
            // Get location if possible for context
            let lat = null, lon = null
            try {
                const pos = await new Promise((resolve, reject) => {
                    navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 })
                })
                lat = pos.coords.latitude
                lon = pos.coords.longitude
            } catch (e) {
                console.log("No location available for chat context")
            }

            const response = await sendChatMessage(userMsg.content, history, lat, lon, "conversational")
            setMessages(prev => [...prev, { role: "model", content: response.response }])
        } catch (err) {
            setMessages(prev => [...prev, { role: "model", content: "Desculpa, ocorreu um erro. Tenta novamente." }])
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="page" style={{ display: 'flex', flexDirection: 'column', height: '100vh', padding: '0', maxWidth: '600px', margin: '0 auto' }}>
            <div style={{ padding: '20px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '15px' }}>
                <a href="/dashboard" style={{ textDecoration: 'none', color: 'var(--text)' }}>← Voltar</a>
                <h2 style={{ margin: 0 }}>AI Stylist</h2>
            </div>
            
            <div style={{ flex: 1, overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '15px' }}>
                {messages.map((msg, i) => {
                    const isUser = msg.role === 'user';
                    // The AI might return JSON updates, let's try to hide them if they exist
                    let displayContent = msg.content;
                    if (!isUser && displayContent.includes('```json')) {
                        displayContent = displayContent.split('```json')[0].trim() || "Armário atualizado!";
                    }

                    return (
                        <div key={i} style={{
                            alignSelf: isUser ? 'flex-end' : 'flex-start',
                            backgroundColor: isUser ? 'var(--accent)' : 'var(--surface)',
                            color: isUser ? '#000' : 'var(--text)',
                            padding: '12px 16px',
                            borderRadius: '12px',
                            maxWidth: '85%',
                            whiteSpace: 'pre-wrap',
                            lineHeight: '1.4'
                        }}>
                            {displayContent}
                        </div>
                    )
                })}
                {loading && (
                    <div style={{ alignSelf: 'flex-start', backgroundColor: 'var(--surface)', padding: '12px 16px', borderRadius: '12px', color: 'var(--muted)' }}>
                        <span className="dot-pulse">A pensar...</span>
                    </div>
                )}
                <div ref={messagesEndRef} />
            </div>

            <form onSubmit={handleSend} style={{ padding: '20px', borderTop: '1px solid var(--border)', display: 'flex', gap: '10px' }}>
                <input 
                    type="text" 
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    placeholder="Ex: Preciso de roupa para um date..."
                    style={{ flex: 1, padding: '14px', borderRadius: '8px', border: '1px solid var(--border)', backgroundColor: 'var(--surface)', color: 'var(--text)' }}
                />
                <button type="submit" disabled={loading || !input.trim()} style={{ padding: '0 20px', height: 'auto', display: 'flex', alignItems: 'center' }}>
                    Enviar
                </button>
            </form>
        </div>
    )
}
