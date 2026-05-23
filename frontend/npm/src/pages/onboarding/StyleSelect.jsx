import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { saveStyles } from "../../services/api"

const STYLES = [
    { id: "streetwear", label: "Streetwear", code: "ST", desc: "Hoodies, sneakers, oversized, urbano" },
    { id: "oldMoney", label: "Old Money", code: "OM", desc: "Loafers, malhas, camisas, classico" },
    { id: "gorpcore", label: "Gorpcore", code: "GC", desc: "Fleece, outdoor, tecnico, utilitario" },
    { id: "preppy", label: "Preppy", code: "PR", desc: "Polos, chinos, blazers, college" },
    { id: "vintage", label: "Vintage", code: "VG", desc: "Retro, thrift, denim, pecas antigas" },
    { id: "minimalist", label: "Minimalista", code: "MN", desc: "Basicos, neutros, cortes limpos" },
    { id: "techwear", label: "Techwear", code: "TW", desc: "Preto, bolsos, nylon, funcional" },
    { id: "y2k", label: "Y2K", code: "Y2", desc: "Anos 2000, metalicos, denim, pop" },
    { id: "luxury", label: "Luxury", code: "LX", desc: "Designer, statement, materiais fortes" },
    { id: "darkAcademia", label: "Dark Academia", code: "DA", desc: "Tweed, la, castanhos, literario" },
    { id: "quietLuxury", label: "Quiet Luxury", code: "QL", desc: "Qualidade discreta, sem logos" },
    { id: "casual", label: "Casual", code: "CS", desc: "Confortavel, simples, dia a dia" },
]

export default function StyleSelect() {
    const [selected, setSelected] = useState(new Set())
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState("")
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
        setError("")
        try {
            const total = selected.size
            const weights = {}
            selected.forEach(id => { weights[id] = +(1 / total).toFixed(2) })
            await saveStyles(weights, null, null, null)
            navigate("/onboarding/wardrobe")
        } catch (err) {
            setError("Nao consegui guardar os estilos: " + err.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="onboarding-container">
            <div className="onboarding-header">
                <div className="step-indicator">Passo 1 de 2</div>
                <h1>Qual e o teu estilo?</h1>
                <p>Seleciona um ou mais estilos para a AI perceber o teu gosto.</p>
            </div>

            <div className="style-grid">
                {STYLES.map(style => (
                    <button
                        key={style.id}
                        className={`style-card ${selected.has(style.id) ? "selected" : ""}`}
                        onClick={() => toggle(style.id)}
                    >
                        <span className="style-emoji">{style.code}</span>
                        <span className="style-label">{style.label}</span>
                        <span className="style-desc">{style.desc}</span>
                        {selected.has(style.id) && <span className="check">ok</span>}
                    </button>
                ))}
            </div>

            {error && <p className="error">{error}</p>}

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
