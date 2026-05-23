import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { saveStyles } from "../../services/api"

const STYLES = [
    { id: "streetwear",   label: "Streetwear",   emoji: "🧢" },
    { id: "oldMoney",     label: "Old Money",     emoji: "🧣" },
    { id: "minimalist",   label: "Minimalist",    emoji: "⬜" },
    { id: "smartCasual",  label: "Smart Casual",  emoji: "👔" },
    { id: "techwear",     label: "Techwear",      emoji: "🖤" },
    { id: "luxury",       label: "Luxury",        emoji: "💎" },
    { id: "vintage",      label: "Vintage",       emoji: "🎞️" },
    { id: "y2k",          label: "Y2K",           emoji: "💿" },
    { id: "scandinavian", label: "Scandinavian",  emoji: "🌿" },
]

export default function StyleSelect() {
    const [selected, setSelected] = useState(new Set())
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()

    function toggle(id) {
        setSelected(prev => {
            const next = new Set(prev)
            next.has(id) ? next.delete(id) : next.add(id)
            return next
        })
    }

    async function handleNext() {
        if (selected.size === 0) return
        setLoading(true)
        try {
            const total = selected.size
            const weights = {}
            selected.forEach(id => { weights[id] = +(1 / total).toFixed(2) })
            await saveStyles(weights, null, null, null)
            navigate("/onboarding/swipe")
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="onboarding-container">
            <div className="onboarding-header">
                <div className="step-indicator">Passo 1 de 2</div>
                <h1>Qual é o teu estilo?</h1>
                <p>Seleciona todos os que te identificas</p>
            </div>

            <div className="style-grid">
                {STYLES.map(style => (
                    <button
                        key={style.id}
                        className={`style-card ${selected.has(style.id) ? "selected" : ""}`}
                        onClick={() => toggle(style.id)}
                    >
                        <span className="style-emoji">{style.emoji}</span>
                        <span className="style-label">{style.label}</span>
                        {selected.has(style.id) && <span className="check">✓</span>}
                    </button>
                ))}
            </div>

            <button
                className="btn-primary btn-next"
                onClick={handleNext}
                disabled={selected.size === 0 || loading}
            >
                {loading ? "A guardar..." : `Continuar (${selected.size} selecionado${selected.size !== 1 ? "s" : ""})`}
            </button>
        </div>
    )
}
