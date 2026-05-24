import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { saveStyles, getStyles } from "../services/api"
import AppHeader from "../components/AppHeader"
import { ArrowLeft } from "../components/Icons"

// Os estilos antigos foram removidos. Agora apresentamos os estilos gerados pelo Tinder-style de forma dinâmica!

export default function SettingsStyles() {
  const [selected, setSelected] = useState(new Set())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const navigate = useNavigate()

  useEffect(() => {
    async function load() {
      try {
        const data = await getStyles()
        if (data && data.styleWeights) {
          const weights = JSON.parse(data.styleWeights)
          // Agora só carregamos as tags tal como vêm da base de dados!
          const tags = Object.keys(weights)
          setSelected(new Set(tags))
        }
      } catch (err) {
        console.error("Failed to load styles:", err)
      }
    }
    load()
  }, [])

  function toggle(id) {
    setSelected(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  async function handleSave() {
    if (selected.size === 0) return
    setLoading(true)
    setError("")
    try {
      const total = selected.size
      const weights = {}
      selected.forEach(id => { weights[id] = +(1 / total).toFixed(2) })
      await saveStyles(weights, null, null, null)
      navigate("/outfits")
    } catch (err) {
      setError("Não foi possível guardar: " + err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-container">
      <AppHeader />

      <button className="back-link" onClick={() => navigate(-1)}>
        <ArrowLeft /> Voltar
      </button>

      <div className="page-header">
        <h1 className="page-title">Preferências de estilo</h1>
        <p className="page-subtitle">O teu Style DNA gerado pela IA</p>
      </div>

      <div style={{ marginBottom: "24px" }}>
        <button 
          className="btn btn-secondary" 
          onClick={() => navigate("/onboarding/styles")}
          style={{ width: "100%", padding: "12px", background: "linear-gradient(135deg, var(--accent) 0%, #d4ff70 100%)", color: "#000", border: "none", fontWeight: "bold", display: "flex", justifyContent: "center", gap: "8px", alignItems: "center" }}
        >
          <span>🔥</span> Refazer Descoberta de Estilo (Tinder-style)
        </button>
      </div>

      <div className="preference-section">
        <h3>As tuas Tags de Estilo</h3>
        <p className="page-subtitle" style={{ marginBottom: "20px" }}>
          Aqui estão os elementos chave que a IA identificou no teu estilo com base no teu Tinder-style:
        </p>
        
        {selected.size === 0 ? (
          <p style={{ color: "var(--color-text-muted)", fontStyle: "italic" }}>Ainda não definiste o teu estilo. Faz o teste acima!</p>
        ) : (
          <div style={{ display: "flex", flexWrap: "wrap", gap: "10px" }}>
            {[...selected].map(tag => (
              <div
                key={tag}
                className="style-chip selected"
                style={{ cursor: "default", textTransform: "capitalize", fontSize: "16px", padding: "8px 16px" }}
              >
                #{tag}
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  )
}
