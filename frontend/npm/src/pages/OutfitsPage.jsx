import { useState, useEffect, useRef } from "react"
import { getOutfits } from "../services/api"
import { useAuth } from "../context/AuthContext"
import AppHeader from "../components/AppHeader"
import { RefreshCw } from "../components/Icons"
import GoogleCalendar from "../components/GoogleCalendar"

const TEMP_LABELS = {
  "very-cold": "Muito frio",
  cold: "Frio",
  cool: "Fresco",
  mild: "Ameno",
  warm: "Quente",
  hot: "Muito quente",
}

const CACHE_KEY = "stylist_outfit_cache"

function loadCache(userId) {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const cached = JSON.parse(raw)
    if (cached.userId !== userId) return null   // different user
    if (!cached.outfit) return null             // empty result, don't persist
    return cached
  } catch {
    return null
  }
}

function saveCache(userId, outfit, weather, advisory) {
  if (!outfit) return   // never cache an empty state
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({
      userId,
      outfit,
      weather,
      advisory,
      savedAt: Date.now()
    }))
  } catch {
    /* storage quota — silently ignore */
  }
}

function clearCache() {
  localStorage.removeItem(CACHE_KEY)
}

export default function OutfitsPage() {
  const { user } = useAuth()
  const [outfit, setOutfit]     = useState(null)
  const [weather, setWeather]   = useState(null)
  const [advisory, setAdvisory] = useState(null)
  const [loading, setLoading]   = useState(true)
  const [regenerating, setRegenerating] = useState(false)
  const [error, setError]       = useState("")
  const coords = useRef(null)

  // On mount: serve cache immediately, only hit the API if nothing is cached
  useEffect(() => {
    const cached = loadCache(user?.id)
    if (cached) {
      setOutfit(cached.outfit)
      setWeather(cached.weather)
      setAdvisory(cached.advisory)
      setLoading(false)
      return
    }
    requestGeolocationThenFetch(false)
  }, [user?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  function requestGeolocationThenFetch(isRegen) {
    if (!navigator.geolocation) {
      fetchOutfit(null, null, isRegen)
      return
    }
    navigator.geolocation.getCurrentPosition(
      pos => {
        coords.current = { lat: pos.coords.latitude, lon: pos.coords.longitude }
        fetchOutfit(coords.current.lat, coords.current.lon, isRegen)
      },
      () => fetchOutfit(null, null, isRegen),
      { timeout: 6000 }
    )
  }

  async function fetchOutfit(lat, lon, isRegen = false) {
    isRegen ? setRegenerating(true) : setLoading(true)
    setError("")
    try {
      const data = await getOutfits(lat, lon)
      const single = (data.outfits ?? [])[0] ?? null
      setOutfit(single)
      setWeather(data.weather ?? null)
      setAdvisory(data.advisory ?? null)
      saveCache(user?.id, single, data.weather ?? null, data.advisory ?? null)
    } catch (err) {
      setError("Erro ao gerar outfit: " + err.message)
    } finally {
      setLoading(false)
      setRegenerating(false)
    }
  }

  function handleRegenerate() {
    clearCache()
    if (coords.current) {
      fetchOutfit(coords.current.lat, coords.current.lon, true)
    } else {
      requestGeolocationThenFetch(true)
    }
  }

  return (
    <div className="app-container">
      <AppHeader />

      <div style={{ display: "flex", gap: "16px", marginBottom: "16px", flexWrap: "wrap", alignItems: "stretch" }}>
        <div style={{ flex: "2 1 400px", minWidth: 0 }}>
          {weather ? <WeatherBar weather={weather} advisory={advisory} /> : <div className="weather-advisory" style={{ padding: "1.5rem" }}>A obter clima...</div>}
        </div>
        <div style={{ flex: "1 1 250px", minWidth: 0 }}>
          <GoogleCalendar />
        </div>
      </div>

      {advisory?.wardrobeAlert && (
        <div className="weather-alert" style={{ marginBottom: "16px" }}>
          <span>⚠️</span>
          <span>{advisory.wardrobeAlert}</span>
        </div>
      )}

      <div className="outfits-section-header">
        <h2 className="outfits-section-title">
          {loading ? "A gerar outfit…" : outfit ? "Outfit para hoje" : "Sem outfit gerado"}
        </h2>
        <button
          className="btn btn-secondary"
          onClick={handleRegenerate}
          disabled={loading || regenerating}
        >
          {regenerating
            ? <><span className="loading-spinner" /> A gerar…</>
            : <><RefreshCw /> Gerar novo</>
          }
        </button>
      </div>

      {loading && (
        <div className="loading-state">
          <div style={{ fontSize: "2rem", marginBottom: "0.5rem" }}>✨</div>
          <p>
            O AI está a combinar as tuas peças
            {weather ? ` para ${Math.round(weather.temperature)}°C em ${weather.city}` : ""}…
          </p>
        </div>
      )}

      {error && (
        <div className="empty-state">
          <div style={{ fontSize: "2rem" }}>⚠️</div>
          <p>{error}</p>
          <button className="btn btn-primary" style={{ marginTop: "1rem" }} onClick={handleRegenerate}>
            Tentar novamente
          </button>
        </div>
      )}

      {!loading && !error && !outfit && (
        <div className="empty-state">
          <div style={{ fontSize: "3rem" }}>👔</div>
          <p style={{ marginBottom: "0.5rem" }}>Ainda não há outfit gerado.</p>
          <p style={{ fontSize: "0.9rem" }}>Adiciona peças ao teu armário primeiro.</p>
          <a href="/wardrobe/upload">
            <button className="btn btn-primary" style={{ marginTop: "1rem" }}>
              Adicionar roupa
            </button>
          </a>
        </div>
      )}

      {!loading && outfit && <OutfitCard outfit={outfit} />}
    </div>
  )
}

function WeatherBar({ weather, advisory }) {
  const firstTip  = advisory?.tips?.[0] ?? null
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
