import { useState, useEffect, useRef } from "react"
import { useSearchParams, useNavigate } from "react-router-dom"
import { getOutfitsForEvent, reserveOutfit } from "../services/api"
import AppHeader from "../components/AppHeader"
import { RefreshCw } from "../components/Icons"

export default function EventOutfits() {
  const [searchParams] = useSearchParams()
  const eventName = searchParams.get("eventName") || "Evento Especial"
  const eventDate = searchParams.get("date") || new Date().toISOString().split("T")[0]
  const displayDate = searchParams.get("displayDate") || eventDate
  
  const [outfits, setOutfits] = useState([])
  const [loading, setLoading] = useState(true)
  const [reservingId, setReservingId] = useState(null)
  const [error, setError] = useState("")
  const navigate = useNavigate()
  const coords = useRef(null)

  useEffect(() => {
    if (!navigator.geolocation) { loadData(null, null); return }
    navigator.geolocation.getCurrentPosition(
      pos => {
        coords.current = { lat: pos.coords.latitude, lon: pos.coords.longitude }
        loadData(coords.current.lat, coords.current.lon)
      },
      () => loadData(null, null),
      { timeout: 6000 }
    )
  }, [eventName, eventDate])

  async function loadData(lat, lon) {
    setLoading(true)
    setError("")
    try {
      const data = await getOutfitsForEvent(eventName, eventDate, lat, lon)
      setOutfits(data.outfits ?? [])
    } catch (err) {
      setError("Erro ao gerar outfits: " + err.message)
    } finally {
      setLoading(false)
    }
  }

  function reload() {
    loadData(coords.current?.lat ?? null, coords.current?.lon ?? null)
  }

  async function handleReserve(outfit, index) {
    setReservingId(index)
    try {
      const itemIds = outfit.items.map(i => i.id)
      await reserveOutfit(eventName, eventDate, itemIds)
      alert("Outfit reservado com sucesso! Estas peças não vão aparecer nas tuas sugestões normais no dia " + eventDate)
      navigate("/dashboard")
    } catch (err) {
      alert("Erro ao reservar: " + err.message)
      setReservingId(null)
    }
  }

  return (
    <div className="app-container">
      <AppHeader />

      <div className="outfits-section-header" style={{ marginTop: "1rem" }}>
        <h2 className="outfits-section-title">
          {loading ? "A preparar looks..." : `Outfits para: ${eventName}`}
        </h2>
        <button className="btn btn-secondary" onClick={reload} disabled={loading}>
          <RefreshCw /> Refazer
        </button>
      </div>

      <div style={{ marginBottom: "2rem", color: "#aaa" }}>
        📅 {displayDate} — Sugestões baseadas no teu armário para a temática deste evento.
      </div>

      {loading && (
        <div className="loading-state">
          <div style={{ fontSize: "2rem", marginBottom: "0.5rem" }}>✨</div>
          <p>O Gemini AI está a criar os melhores outfits para o teu evento…</p>
        </div>
      )}

      {error && (
        <div className="empty-state">
          <div style={{ fontSize: "2rem" }}>⚠️</div>
          <p>{error}</p>
          <button className="btn btn-primary" style={{ marginTop: "1rem" }} onClick={reload}>
            Tentar novamente
          </button>
        </div>
      )}

      {!loading && !error && outfits.length === 0 && (
        <div className="empty-state">
          <div style={{ fontSize: "3rem" }}>👔</div>
          <p style={{ marginBottom: "0.5rem" }}>Não consegui gerar outfits para este evento.</p>
          <p style={{ fontSize: "0.9rem" }}>Tenta adicionar peças mais adequadas ao teu armário.</p>
        </div>
      )}

      {!loading && outfits.length > 0 && (
        <div className="outfits-grid">
          {outfits.map((outfit, i) => (
            <EventOutfitCard 
              key={i} 
              outfit={outfit} 
              isReserving={reservingId === i}
              onReserve={() => handleReserve(outfit, i)}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function EventOutfitCard({ outfit, isReserving, onReserve }) {
  return (
    <div className="outfit-card" style={{ border: "2px solid rgba(255, 255, 255, 0.1)" }}>
      <div className="outfit-card-header">
        <div className="outfit-card-name" style={{ color: "#e1a8ff" }}>{outfit.name}</div>
        <div className="outfit-card-desc">{outfit.description}</div>
      </div>
      <div className="outfit-card-items">
        {outfit.items.map(item => (
          <div key={item.id} className="outfit-card-item">
            <img src={`http://localhost:8080${item.imageUrl}`} alt={item.subcategory} loading="lazy" />
            <div className="outfit-card-item-label">{item.color} {item.subcategory}</div>
          </div>
        ))}
      </div>
      <div style={{ padding: "1rem", borderTop: "1px solid rgba(255,255,255,0.05)" }}>
        <button 
          className="btn btn-primary" 
          style={{ width: "100%" }}
          onClick={onReserve}
          disabled={isReserving}
        >
          {isReserving ? "A reservar..." : "Reservar este Outfit"}
        </button>
      </div>
    </div>
  )
}
