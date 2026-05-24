import { useState, useEffect, useRef } from "react"
import { motion } from "framer-motion"
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
  const [toastMessage, setToastMessage] = useState(null)
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
      setToastMessage("Outfit reservado! Estas peças não aparecerão nas tuas sugestões de " + eventDate)
      setTimeout(() => navigate("/dashboard"), 2500)
    } catch (err) {
      setToastMessage("⚠️ Erro ao reservar: " + err.message)
      setReservingId(null)
      setTimeout(() => setToastMessage(null), 3000)
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
          <p>O Groq está a criar os melhores outfits para o teu evento…</p>
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

      {toastMessage && (
        <div style={{
          position: "fixed", bottom: "2rem", right: "2rem",
          background: "var(--color-bg)", padding: "1rem 1.5rem", borderRadius: "8px",
          borderLeft: "4px solid var(--color-primary)", boxShadow: "0 8px 30px rgba(0,0,0,0.5)",
          color: "#fff", zIndex: 1000, display: "flex", alignItems: "center", gap: "12px",
          animation: "slideIn 0.3s ease-out forwards"
        }}>
          <span style={{ fontSize: "1.2rem" }}>✅</span>
          <div>
            <div style={{ fontWeight: "bold", marginBottom: "4px" }}>Sucesso</div>
            <div style={{ fontSize: "0.9rem", color: "#aaa" }}>{toastMessage}</div>
          </div>
        </div>
      )}
    </div>
  )
}

const CATEGORY_SLOT = {
  tops:        "top",
  jackets:     "jacket",
  bottoms:     "bottoms",
  accessories: "access",
  shoes:       "shoes",
}

function FlatLayItem({ item, slot, delay = 0 }) {
  return (
    <motion.div
      className={`flat-lay-item flat-lay-${slot}`}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: "spring", stiffness: 280, damping: 26, delay }}
    >
      <img src={`http://localhost:8080${item.imageUrl}`} alt={item.subcategory} loading="lazy" />
      <div className="flat-lay-label">
        <span className="flat-lay-label-name">{item.color} {item.subcategory}</span>
        <span className="tag tag-category">{item.category}</span>
      </div>
    </motion.div>
  )
}

function EventOutfitCard({ outfit, isReserving, onReserve }) {
  const slots = {}
  const unslotted = []
  for (const item of outfit.items) {
    const slot = CATEGORY_SLOT[item.category]
    if (slot && !slots[slot]) slots[slot] = item
    else if (!slot) unslotted.push(item)
  }

  const showTop    = !!slots.top
  const showJacket = !!slots.jacket
  const showBottom = !!slots.bottoms
  const showAccess = !!slots.access
  const showShoes  = !!slots.shoes

  return (
    <motion.div 
      className="outfit-card"
      initial={{ opacity: 0, scale: 0.97 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
    >
      <div className="outfit-card-header">
        <div className="outfit-card-name" style={{ color: "var(--color-primary)" }}>{outfit.name}</div>
        <div className="outfit-card-desc">{outfit.description}</div>
      </div>
      
      <div className="flat-lay-grid">
        {showTop    && <FlatLayItem item={slots.top}    slot="top"    delay={0.05} />}
        {showJacket && <FlatLayItem item={slots.jacket} slot="jacket" delay={0.10} />}
        {showBottom && <FlatLayItem item={slots.bottoms} slot="bottoms" delay={0.15} />}
        {showAccess && <FlatLayItem item={slots.access} slot="access" delay={0.20} />}
        {showShoes  && <FlatLayItem item={slots.shoes}  slot="shoes"  delay={0.25} />}
        {unslotted.map((item, i) => (
          <FlatLayItem key={item.id} item={item} slot="top" delay={0.05 * i} />
        ))}
      </div>

      <div style={{ padding: "1rem", borderTop: "1px solid rgba(255,255,255,0.05)" }}>
        <button 
          className="btn btn-primary" 
          style={{ width: "100%", padding: "0.8rem", fontWeight: "bold" }}
          onClick={onReserve}
          disabled={isReserving}
        >
          {isReserving ? "A reservar..." : "Reservar este Outfit para o Evento"}
        </button>
      </div>
    </motion.div>
  )
}
