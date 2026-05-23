import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { saveStyles, getStyles } from "../services/api"
import AppHeader from "../components/AppHeader"
import { ArrowLeft } from "../components/Icons"

const STYLES = [
  { id: "streetwear",   label: "Streetwear",     desc: "Hoodies, sneakers, oversized, urbano" },
  { id: "oldMoney",     label: "Old Money",       desc: "Loafers, malhas, camisas, clássico" },
  { id: "gorpcore",     label: "Gorpcore",        desc: "Fleece, outdoor, técnico, utilitário" },
  { id: "preppy",       label: "Preppy",          desc: "Polos, chinos, blazers, college" },
  { id: "vintage",      label: "Vintage",         desc: "Retro, thrift, denim, peças antigas" },
  { id: "minimalist",   label: "Minimalista",     desc: "Básicos, neutros, cortes limpos" },
  { id: "techwear",     label: "Techwear",        desc: "Preto, bolsos, nylon, funcional" },
  { id: "y2k",          label: "Y2K",             desc: "Anos 2000, metálicos, denim, pop" },
  { id: "luxury",       label: "Luxury",          desc: "Designer, statement, materiais fortes" },
  { id: "darkAcademia", label: "Dark Academia",   desc: "Tweed, lã, castanhos, literário" },
  { id: "quietLuxury",  label: "Quiet Luxury",    desc: "Qualidade discreta, sem logos" },
  { id: "casual",       label: "Casual",          desc: "Confortável, simples, dia a dia" },
]

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
          // Filtra para garantir que só carregamos IDs que existem na lista STYLES atual
          const validIds = Object.keys(weights).filter(id => 
            STYLES.some(s => s.id === id)
          )
          setSelected(new Set(validIds))
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
        <p className="page-subtitle">Seleciona os estilos que o AI vai usar nas tuas sugestões</p>
      </div>

      <div className="preference-section">
        <h3>Os meus estilos</h3>
        <p className="page-subtitle" style={{ marginBottom: "20px" }}>
          Seleciona um ou mais estilos que te identificam
        </p>
        <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
          {STYLES.map(style => (
            <button
              key={style.id}
              className={`style-chip ${selected.has(style.id) ? "selected" : ""}`}
              onClick={() => toggle(style.id)}
              title={style.desc}
            >
              {style.label}
            </button>
          ))}
        </div>
      </div>

      {selected.size > 0 && (
        <div className="preference-section">
          <h3>Selecionados</h3>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
            {[...selected].map(id => {
              const s = STYLES.find(st => st.id === id)
              return (
                <div key={id} style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                  <span style={{ fontSize: "13px", fontWeight: "500" }}>{s?.label}</span>
                  <span style={{ fontSize: "12px", color: "var(--color-text-muted)" }}>{s?.desc}</span>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {error && <p className="error">{error}</p>}

      <button
        className="btn btn-primary"
        onClick={handleSave}
        disabled={selected.size === 0 || loading}
        style={{ width: "100%", marginTop: "8px" }}
      >
        {loading
          ? <><span className="loading-spinner" /> A guardar…</>
          : `Guardar preferências (${selected.size} estilo${selected.size !== 1 ? "s" : ""})`
        }
      </button>
    </div>
  )
}
