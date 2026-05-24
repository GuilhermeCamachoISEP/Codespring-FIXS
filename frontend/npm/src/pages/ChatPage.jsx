import { useState, useRef, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { sendChatMessage } from "../services/api"
import { useAuth } from "../context/AuthContext"
import AppHeader from "../components/AppHeader"
import { ArrowLeft } from "../components/Icons"

export default function ChatPage() {
    const { user } = useAuth()
    const navigate = useNavigate()
    const [messages, setMessages] = useState([
        { role: "model", content: "Olá! Diz-me que peças novas queres adicionar ao teu armário virtual (ex: 'adicionei uns ténis brancos da Nike e uma t-shirt preta básica')." }
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
            // Strip ```json blocks from AI history before sending — prevents the AI from
            // re-emitting previously-confirmed items in the new turn and double-saving them.
            const history = messages
                .filter(m => m.role !== "model" || !m.content.startsWith("Olá! Sou o teu assistente de armário"))
                .map(m => {
                    if (m.role !== "model") return m
                    const stripped = m.content.includes("```json")
                        ? (m.content.split("```json")[0].trim() || "Armário atualizado!")
                        : m.content
                    return { ...m, content: stripped }
                })
            
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
        <div className="app-container">
            <AppHeader />
            
            <button className="back-link" onClick={() => navigate("/wardrobe")}>
                <ArrowLeft /> Voltar ao Armário
            </button>

            <div className="page-header" style={{ marginBottom: "10px" }}>
                <h1 className="page-title">Adição Rápida (Por Texto)</h1>
                <p className="page-subtitle">A IA extrai a categoria, cor e o nome das roupas que escreveres</p>
            </div>
            
            <div style={{ flex: 1, overflowY: 'auto', padding: '10px 0', display: 'flex', flexDirection: 'column', gap: '15px', minHeight: '50vh' }}>
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

            <form onSubmit={handleSend} style={{ paddingTop: '15px', borderTop: '1px solid var(--border)', display: 'flex', gap: '10px', marginTop: 'auto' }}>
                <input 
                    type="text" 
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    placeholder="Ex: Adiciona uma t-shirt branca da Nike..."
                    style={{ flex: 1, padding: '14px', borderRadius: '8px', border: '1px solid var(--border)', backgroundColor: 'var(--surface)', color: 'var(--text)' }}
                />
                <button type="submit" disabled={loading || !input.trim()} style={{ padding: '0 20px', height: 'auto', display: 'flex', alignItems: 'center' }}>
                    Enviar
                </button>
            </form>
        </div>
    )
}
