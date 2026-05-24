import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { getInspiration } from "../services/api"
import AppHeader from "../components/AppHeader"
import { ArrowLeft, Images } from "../components/Icons"

export default function InspirationPage() {
  const navigate = useNavigate()

  const [sections, setSections]                     = useState([])
  const [loading, setLoading]                       = useState(true)
  const [configured, setConfigured]                 = useState(true)
  const [error, setError]                           = useState("")

  useEffect(() => {
    getInspiration()
      .then(data => {
        setSections(data.sections ?? [])
        setConfigured(data.configured ?? false)
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="app-container">
      <AppHeader />

      <button className="back-link" onClick={() => navigate(-1)}>
        <ArrowLeft /> Voltar
      </button>

      <div className="page-header">
        <h1 className="page-title">Inspiração</h1>
        <p className="page-subtitle">Looks baseados no teu estilo</p>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "20px" }}>
        <Images style={{ width: 20, height: 20, color: "var(--color-accent)" }} />
        <h2 style={{ margin: 0, fontSize: "1.1rem", fontWeight: 600 }}>
          Looks do teu estilo
        </h2>
        <span style={{ fontSize: "0.75rem", color: "var(--color-text-muted)", marginLeft: "auto" }}>
          via Unsplash
        </span>
      </div>

      {loading && (
        <div className="loading-state">
          <div style={{ fontSize: "1.5rem", marginBottom: "0.5rem" }}>✨</div>
          <p>A procurar inspiração para o teu estilo…</p>
        </div>
      )}

      {!loading && !configured && (
        <div className="empty-state">
          <div style={{ fontSize: "2rem" }}>🖼️</div>
          <p>Unsplash não configurado.</p>
          <p style={{ fontSize: "0.85rem", color: "var(--color-text-muted)" }}>
            Adiciona a <code>UNSPLASH_ACCESS_KEY</code> no ficheiro <code>.env</code> do backend.<br />
            Chave gratuita em{" "}
            <a href="https://unsplash.com/developers" target="_blank" rel="noreferrer"
               style={{ color: "var(--color-accent)" }}>
              unsplash.com/developers
            </a>
          </p>
        </div>
      )}

      {!loading && error && (
        <div className="empty-state">
          <div style={{ fontSize: "2rem" }}>⚠️</div>
          <p>{error}</p>
        </div>
      )}

      {!loading && configured && sections.length === 0 && !error && (
        <div className="empty-state">
          <p>Nenhuma foto encontrada. Define as tuas preferências de estilo primeiro.</p>
          <button className="btn btn-primary" style={{ marginTop: "1rem" }}
                  onClick={() => navigate("/preferences")}>
            Definir estilo
          </button>
        </div>
      )}

      {sections.map(section => (
        <div key={section.styleId} style={{ marginBottom: "32px" }}>
          <h3 style={{
            fontSize: "1rem", fontWeight: 600, marginBottom: "12px",
            color: "var(--color-text)", borderBottom: "1px solid var(--color-border)",
            paddingBottom: "8px"
          }}>
            {section.styleLabel}
          </h3>
          <div className="inspiration-grid">
            {section.photos.map(photo => (
              <a
                key={photo.id}
                href={`${photo.unsplashUrl}?utm_source=stylist_ai&utm_medium=referral`}
                target="_blank"
                rel="noreferrer"
                className="inspiration-card"
              >
                <img src={photo.thumbUrl} alt={photo.styleLabel} loading="lazy" />
                <div className="inspiration-card-overlay">
                  <span className="inspiration-photo-credit">📷 {photo.photographerName}</span>
                </div>
              </a>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
