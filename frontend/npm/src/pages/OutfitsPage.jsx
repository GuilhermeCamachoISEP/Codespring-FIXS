import { useState, useEffect, useRef } from "react"
import { getOutfits, saveOutfitHistory } from "../services/api"
import AppHeader from "../components/AppHeader"
import { RefreshCw } from "../components/Icons"

const TEMP_LABELS = {
  "very-cold": "Muito frio",
  cold: "Frio",
  cool: "Fresco",
  mild: "Ameno",
  warm: "Quente",
  hot: "Muito quente",
}

export default function OutfitsPage() {
  const [outfits, setOutfits] = useState([])
  const [weather, setWeather] = useState(null)
  const [advisory, setAdvisory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)
  const [error, setError] = useState("")
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
  }, [])

  async function loadData(lat, lon, isRegen = false) {
    isRegen ? setRegenerating(true) : setLoading(true)
    setError("")
    try {
      const data = await getOutfits(lat, lon)
      setOutfits(data.outfits ?? [])
      setWeather(data.weather ?? null)
      setAdvisory(data.advisory ?? null)
      
      // Auto-save generated outfits asynchronously
      if (data.outfits && data.outfits.length > 0) {
        Promise.all(data.outfits.map(o => saveOutfitHistory(o.items)))
            .catch(e => console.error("Failed to save outfit history", e))
      }
    } catch (err) {
      setError("Erro ao gerar outfits: " + err.message)
    } finally {
      setLoading(false)
      setRegenerating(false)
    }
  }

  function reload() {
    loadData(coords.current?.lat ?? null, coords.current?.lon ?? null, true)
  }

  return (
    <div className="app-container">
      <AppHeader />

      {weather && <WeatherBar weather={weather} advisory={advisory} />}

      {advisory?.wardrobeAlert && (
        <div className="weather-alert" style={{ marginBottom: "16px" }}>
          <span>⚠️</span>
          <span>{advisory.wardrobeAlert}</span>
        </div>
      )}

      <div className="outfits-section-header">
        <h2 className="outfits-section-title">
          {loading ? "A gerar outfits…" : `${outfits.length} outfit${outfits.length !== 1 ? "s" : ""} para hoje`}
        </h2>
        <button
          className="btn btn-secondary"
          onClick={reload}
          disabled={loading || regenerating}
        >
          {regenerating ? (
            <><span className="loading-spinner" /> A gerar…</>
          ) : (
            <><RefreshCw /> Gerar novos</>
          )}
        </button>
      </div>

      {loading && (
        <div className="loading-state">
          <div style={{ fontSize: "2rem", marginBottom: "0.5rem" }}>✨</div>
          <p>
            O Gemini AI está a combinar as tuas peças
            {weather ? ` para ${Math.round(weather.temperature)}°C em ${weather.city}` : ""}…
          </p>
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
          <p style={{ marginBottom: "0.5rem" }}>Ainda não há outfits gerados.</p>
          <p style={{ fontSize: "0.9rem" }}>Adiciona peças ao teu armário primeiro.</p>
          <a href="/wardrobe/upload">
            <button className="btn btn-primary" style={{ marginTop: "1rem" }}>
              Adicionar roupa
            </button>
          </a>
        </div>
      )}

      {!loading && outfits.length > 0 && (
        <div className="outfits-grid">
          {outfits.map((outfit, i) => (
            <OutfitCard key={i} outfit={outfit} />
          ))}
        </div>
      )}
    </div>
  )
}

function WeatherBar({ weather, advisory }) {
  const firstTip = advisory?.tips?.[0] ?? null
  const extraTips = advisory?.tips?.slice(1) ?? []

  return (
    <div className="weather-advisory" style={{ marginBottom: "16px" }}>
      <div className="context-bar" style={{ marginBottom: 0, boxShadow: "none", background: "transparent", padding: "1.1rem 1.5rem" }}>
        <div className="weather-bar-left">
          <div className="weather-bar-icon">{weather.icon}</div>
          <div className="weather-bar-info">
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <span className="weather-bar-temp">{Math.round(weather.temperature)}°C</span>
              <span className={`weather-badge weather-badge-${weather.tempCategory}`}>
                {TEMP_LABELS[weather.tempCategory] ?? "Variável"}
              </span>
            </div>
            <div className="weather-bar-desc">
              {weather.description}
              {weather.windSpeed > 20 && ` · ${Math.round(weather.windSpeed)} km/h`}
            </div>
            <div className="weather-bar-city">📍 {weather.city}</div>
          </div>
        </div>
        {firstTip && (
          <>
            <div className="context-divider" />
            <div className="weather-bar-tip">{firstTip}</div>
          </>
        )}
      </div>

      {extraTips.length > 0 && (
        <div className="weather-tips">
          <div className="weather-tips-label">Dicas para hoje</div>
          {extraTips.map((tip, i) => (
            <div key={i} className="weather-tip">{tip}</div>
          ))}
        </div>
      )}
    </div>
  )
}

function OutfitCard({ outfit }) {
  return (
    <div className="outfit-card">
      <div className="outfit-card-header">
        <div className="outfit-card-name">{outfit.name}</div>
        <div className="outfit-card-desc">{outfit.description}</div>
        {outfit.weatherNote && (
          <div className="outfit-weather-note">
            <span>🌡️</span>
            <span>{outfit.weatherNote}</span>
          </div>
        )}
      </div>
      <div className="outfit-card-items">
        {outfit.items.map(item => (
          <div key={item.id} className="outfit-card-item">
            <img
              src={item.imageUrl?.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`}
              alt={item.subcategory}
              loading="lazy"
            />
            <div className="outfit-card-item-label">
                <span>{item.color} {item.subcategory}</span>
                <span className="tag tag-category" style={{marginLeft: "4px"}}>{item.category}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
