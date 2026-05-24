import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { motion } from "framer-motion"
import { planPacking } from "../services/api"
import AppHeader from "../components/AppHeader"

// PITCH NOTE: This feature proves the wardrobe has value beyond daily use.
// It answers "why would I bother building a digital wardrobe?" with a
// concrete, immediately useful answer everyone has experienced.
// During demo: type "4 dias em Lisboa, reunião de negócios e fim de semana"
// The AI will pull from the seeded demo wardrobe and show a real packing plan.
// Highlight the versatility stars — the blazer will appear in 3+ outfits.
// Say: "One AI. Your actual clothes. Zero overpacking."

const LOADING_MESSAGES = [
  "A ler o teu armário...",
  "A encontrar as peças mais versáteis...",
  "A construir o teu plano de viagem...",
  "A calcular as combinações possíveis..."
]

export default function PackingPage() {
    const navigate = useNavigate()
    const [input, setInput] = useState("")
    const [travelDate, setTravelDate] = useState("")
    const [loading, setLoading] = useState(false)
    const [loadingMsgIdx, setLoadingMsgIdx] = useState(0)
    const [error, setError] = useState("")
    const [result, setResult] = useState(null)
    const [savedPlans, setSavedPlans] = useState([])
    const [saveToast, setSaveToast] = useState(null) // "success" | "error" | null

    useEffect(() => {
        loadSavedPlans()
    }, [])

    useEffect(() => {
        let interval;
        if (loading) {
            interval = setInterval(() => {
                setLoadingMsgIdx(prev => (prev + 1) % LOADING_MESSAGES.length);
            }, 1500);
        }
        return () => clearInterval(interval);
    }, [loading]);

    function loadSavedPlans() {
        try {
            const raw = localStorage.getItem("packing_plans")
            if (raw) setSavedPlans(JSON.parse(raw))
        } catch { }
    }

    async function handleGenerate(e) {
        e.preventDefault()
        if (!input.trim()) return

        setLoading(true)
        setError("")
        setResult(null)
        setLoadingMsgIdx(0)

        try {
            const data = await planPacking(input, travelDate)
            // Add original input to data so we can save it later
            data.tripDescription = input
            data.travelDate = travelDate
            setResult(data)
        } catch (err) {
            setError(err.message || "Erro ao planear a viagem.")
        } finally {
            setLoading(false)
        }
    }

    function savePlan() {
        if (!result) return
        try {
            const newPlan = { ...result, savedAt: new Date().toISOString() }
            const updated = [newPlan, ...savedPlans]
            localStorage.setItem("packing_plans", JSON.stringify(updated))
            setSavedPlans(updated)
            setSaveToast("success")
            setTimeout(() => setSaveToast(null), 3000)
        } catch {
            setSaveToast("error")
            setTimeout(() => setSaveToast(null), 3000)
        }
    }

    function deletePlan(index) {
        const updated = [...savedPlans]
        updated.splice(index, 1)
        localStorage.setItem("packing_plans", JSON.stringify(updated))
        setSavedPlans(updated)
    }

    function loadPlan(plan) {
        setResult(plan)
        setInput(plan.tripDescription || "")
    }

    return (
        <div className="app-container">
            <AppHeader />

            <div className="page-header" style={{ marginTop: "1rem" }}>
                <h1 className="page-title">Mala Inteligente 🧳</h1>
                <p className="page-subtitle">Diz-nos para onde vais. A IA escolhe o que levar do teu armário.</p>
            </div>

            {!result && !loading && (
                <div style={{ maxWidth: "600px", margin: "0 auto" }}>
                    <form onSubmit={handleGenerate} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                            <label style={{ fontWeight: "600" }}>Quando vais viajar?</label>
                            <input 
                                type="date" 
                                className="search-input" 
                                style={{ padding: "12px", borderRadius: "8px", fontSize: "1rem" }}
                                value={travelDate}
                                onChange={(e) => setTravelDate(e.target.value)}
                            />
                        </div>
                        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                            <label style={{ fontWeight: "600" }}>Detalhes da viagem</label>
                            <textarea
                                className="search-input"
                                style={{ minHeight: "120px", resize: "vertical", padding: "16px", borderRadius: "12px", fontSize: "1.1rem" }}
                                placeholder="Descreve a tua viagem... ex: 4 dias em Londres, um jantar formal e dois dias de trabalho."
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                            ></textarea>
                        </div>
                        <button className="btn btn-primary" type="submit" style={{ padding: "14px", fontSize: "1.1rem" }}>
                            Planear a mala
                        </button>
                        <p style={{ textAlign: "center", color: "var(--color-text-muted)", fontSize: "0.85rem", marginTop: "-8px" }}>
                            A IA vai adaptar as peças ao clima do destino na data escolhida.
                        </p>
                    </form>

                    {error && (
                        <div className="weather-alert" style={{ marginTop: "16px", backgroundColor: "#ffebee", color: "#c62828" }}>
                            <span>⚠️</span>
                            <span>{error}</span>
                        </div>
                    )}

                    {savedPlans.length > 0 && (
                        <div style={{ marginTop: "40px" }}>
                            <h3 style={{ marginBottom: "16px", fontSize: "1.2rem" }}>Planos Guardados</h3>
                            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                                {savedPlans.map((plan, i) => (
                                    <div key={i} style={{ padding: "16px", backgroundColor: "var(--color-surface)", borderRadius: "12px", border: "1px solid var(--color-border)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                        <div>
                                            <div style={{ fontWeight: "600", marginBottom: "4px" }}>{plan.tripDescription}</div>
                                            <div style={{ fontSize: "0.85rem", color: "var(--color-text-muted)" }}>
                                                {plan.travelDate ? `Viaja a ${plan.travelDate} • ` : ""}Guardado em {new Date(plan.savedAt).toLocaleDateString("pt-PT")} • {plan.totalItems} peças
                                            </div>
                                        </div>
                                        <div style={{ display: "flex", gap: "8px" }}>
                                            <button className="btn btn-secondary" onClick={() => loadPlan(plan)}>Ver</button>
                                            <button className="btn btn-secondary" onClick={() => deletePlan(i)}>X</button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {loading && (
                <div className="loading-state" style={{ marginTop: "40px" }}>
                    <div className="ai-pulse-ring"><div className="ai-core"></div></div>
                    <p className="loading-text" style={{ minHeight: '1.5em' }}>{LOADING_MESSAGES[loadingMsgIdx]}</p>
                </div>
            )}

            {result && !loading && (
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "24px", flexWrap: "wrap", gap: "16px" }}>
                        <button className="btn btn-secondary" onClick={() => setResult(null)}>← Voltar</button>
                        <h2 style={{ fontSize: "1.5rem", margin: 0 }}>Plano de Viagem</h2>
                        <button className="btn btn-primary" onClick={savePlan}>Guardar plano</button>
                    </div>

                    <div style={{ display: "flex", flexWrap: "wrap", gap: "32px", alignItems: "flex-start" }}>
                        {/* LEFT SECTION - O que levar */}
                        <div style={{ flex: "1 1 300px", backgroundColor: "var(--color-surface)", padding: "24px", borderRadius: "16px", border: "1px solid var(--color-border)" }}>
                            <h3 style={{ fontSize: "1.3rem", marginBottom: "8px" }}>O que levar</h3>
                            <p style={{ color: "var(--color-text-muted)", marginBottom: "16px", fontWeight: "500" }}>{result.totalItems} peças. Até {result.dayPlans?.length * 3 || 0} combinações.</p>
                            
                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(100px, 1fr))", gap: "16px" }}>
                                {result.packingList?.map(item => (
                                    <div key={item.itemId} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "8px", padding: "8px", backgroundColor: "var(--color-bg)", borderRadius: "8px" }}>
                                        <div style={{ width: "80px", height: "80px", borderRadius: "8px", overflow: "hidden", backgroundColor: "#fff", display: "flex", alignItems: "center", justifyContent: "center" }}>
                                            {item.imageUrl ? (
                                                <img src={item.imageUrl.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`} alt={item.itemName} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                            ) : (
                                                <span style={{ fontSize: "2rem" }}>👕</span>
                                            )}
                                        </div>
                                        <div style={{ fontSize: "0.8rem", textAlign: "center", fontWeight: "500", lineHeight: "1.2" }}>{item.itemName}</div>
                                    </div>
                                ))}
                            </div>
                            
                            <p style={{ marginTop: "24px", fontStyle: "italic", fontSize: "0.95rem", color: "var(--color-text-muted)", lineHeight: "1.5", borderLeft: "3px solid var(--accent)", paddingLeft: "12px" }}>
                                {result.aiReasoning}
                            </p>
                        </div>

                        {/* RIGHT SECTION - Plano por dia */}
                        <div style={{ flex: "2 1 400px" }}>
                            <h3 style={{ fontSize: "1.3rem", marginBottom: "24px" }}>Plano por dia</h3>
                            <div style={{ display: "flex", flexDirection: "column", gap: "24px", position: "relative" }}>
                                <div style={{ position: "absolute", left: "24px", top: "24px", bottom: "24px", width: "2px", backgroundColor: "var(--color-border)", zIndex: 0 }}></div>
                                
                                {result.dayPlans?.map((day, i) => (
                                    <div key={i} style={{ display: "flex", gap: "16px", position: "relative", zIndex: 1 }}>
                                        <div style={{ width: "48px", height: "48px", borderRadius: "50%", backgroundColor: "var(--accent)", color: "#000", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: "bold", flexShrink: 0, border: "4px solid var(--color-bg)" }}>
                                            D{day.dayNumber}
                                        </div>
                                        <div style={{ flex: 1, backgroundColor: "var(--color-surface)", padding: "20px", borderRadius: "16px", border: "1px solid var(--color-border)" }}>
                                            <div style={{ fontWeight: "600", fontSize: "1.1rem", marginBottom: "12px" }}>{day.context}</div>
                                            
                                            <div style={{ display: "flex", gap: "8px", marginBottom: "16px", flexWrap: "wrap" }}>
                                                {day.items.map((itemId, j) => {
                                                    const packed = result.packingList?.find(p => p.itemId === itemId)
                                                    if (!packed) return null
                                                    return (
                                                        <div key={j} style={{ width: "50px", height: "50px", borderRadius: "8px", overflow: "hidden", backgroundColor: "var(--color-bg)", border: "1px solid var(--color-border)" }} title={packed.itemName}>
                                                            {packed.imageUrl ? (
                                                                <img src={packed.imageUrl.startsWith("http") ? packed.imageUrl : `http://localhost:8080${packed.imageUrl}`} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                                            ) : null}
                                                        </div>
                                                    )
                                                })}
                                            </div>
                                            
                                            <p style={{ color: "var(--color-text-muted)", fontSize: "0.95rem", lineHeight: "1.5", margin: 0 }}>{day.outfitDescription}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </motion.div>
            )}

            {saveToast && (
                <div style={{
                    position: "fixed", bottom: "2rem", left: "50%", transform: "translateX(-50%)",
                    display: "flex", alignItems: "center", gap: "10px",
                    background: saveToast === "success" ? "var(--color-success, #22c55e)" : "var(--color-danger, #ef4444)",
                    color: "#000", padding: "12px 24px", borderRadius: "999px",
                    fontWeight: 600, fontSize: "0.95rem",
                    boxShadow: "0 4px 20px rgba(0,0,0,0.4)", zIndex: 9999,
                    animation: "fadeInUp 0.25s ease-out"
                }}>
                    {saveToast === "success" ? "✓ Plano guardado com sucesso!" : "⚠️ Erro ao guardar o plano."}
                </div>
            )}
        </div>
    )
}
