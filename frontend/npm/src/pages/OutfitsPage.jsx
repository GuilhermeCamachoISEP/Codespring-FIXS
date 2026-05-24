import { useState, useEffect, useRef } from "react"
import { motion } from "framer-motion"
import { getOutfits, getWardrobe, getWeather, saveOutfitHistory, refineOutfit } from "../services/api"
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

const LOADING_MESSAGES = [
  "A analisar a meteorologia...",
  "A verificar os teus eventos...",
  "A cruzar com o teu armário...",
  "A aplicar o teu Style DNA 🧬..."
]

const GEO_TIMEOUT_MS = 8000
const WEATHER_TIMEOUT_MS = 8000

function resolveGeolocation(timeoutMs = GEO_TIMEOUT_MS) {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({ lat: null, lon: null })
      return
    }
    const timer = setTimeout(() => resolve({ lat: null, lon: null }), timeoutMs)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        clearTimeout(timer)
        resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude })
      },
      () => {
        clearTimeout(timer)
        resolve({ lat: null, lon: null })
      },
      { timeout: timeoutMs, maximumAge: 300000, enableHighAccuracy: false }
    )
  })
}

export default function OutfitsPage() {
  const { user } = useAuth()
  const [outfit, setOutfit]     = useState(null)
  const [weather, setWeather]   = useState(null)
  const [advisory, setAdvisory] = useState(null)
  const [loading, setLoading]   = useState(true)
  const [weatherLoading, setWeatherLoading] = useState(false)
  const [weatherUnavailable, setWeatherUnavailable] = useState(false)
  const [loadingMsgIdx, setLoadingMsgIdx] = useState(0)
  const [regenerating, setRegenerating] = useState(false)
  const [error, setError]       = useState("")
  const [wardrobeItems, setWardrobeItems] = useState([])
  const [refinementInput, setRefinementInput] = useState("")
  const [refinementHistory, setRefinementHistory] = useState([])
  const [refining, setRefining] = useState(false)
  const [refineError, setRefineError] = useState("")
  const coords = useRef(null)
  const weatherRef = useRef(null)
  const lastSavedOutfitStr = useRef("")

  weatherRef.current = weather

  useEffect(() => {
    let interval;
    if (loading || regenerating) {
      interval = setInterval(() => {
        setLoadingMsgIdx(prev => (prev + 1) % LOADING_MESSAGES.length);
      }, 1500); // Change message every 1.5s
    }
    return () => clearInterval(interval);
  }, [loading, regenerating]);

  // Fallback: never leave weather bar stuck on "A obter clima..."
  useEffect(() => {
    if (!weatherLoading) return
    const timer = setTimeout(() => {
      setWeatherLoading(false)
      if (!weatherRef.current) setWeatherUnavailable(true)
    }, WEATHER_TIMEOUT_MS)
    return () => clearTimeout(timer)
  }, [weatherLoading])

  // On mount: serve cache immediately, only hit the API if nothing is cached
  useEffect(() => {
    const cached = loadCache(user?.id)
    if (cached) {
      setOutfit(cached.outfit)
      setWeather(cached.weather ?? null)
      setAdvisory(cached.advisory)
      setWeatherLoading(false)
      setWeatherUnavailable(!cached.weather)
      setLoading(false)
      if (cached.outfit) loadWardrobe()
      return
    }
    requestGeolocationThenFetch(false)
  }, [user?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  async function requestGeolocationThenFetch(isRegen) {
    setWeatherLoading(true)
    setWeatherUnavailable(false)
    const { lat, lon } = await resolveGeolocation(GEO_TIMEOUT_MS)
    if (lat != null && lon != null) {
      coords.current = { lat, lon }
    } else {
      coords.current = null
    }
    await fetchOutfit(lat, lon, isRegen)
  }

  async function fetchWeather(lat, lon) {
    if (lat == null || lon == null) {
      setWeatherLoading(false)
      setWeatherUnavailable(true)
      return
    }
    setWeatherLoading(true)
    try {
      const data = await getWeather(lat, lon)
      setWeather(data)
      setWeatherUnavailable(false)
    } catch (err) {
      console.warn("Weather fetch failed:", err.message)
      setWeatherUnavailable(true)
    } finally {
      setWeatherLoading(false)
    }
  }

  function applyWeatherFromOutfitsResponse(data) {
    if (data.weather) {
      setWeather(data.weather)
      setWeatherUnavailable(false)
      setWeatherLoading(false)
      return true
    }
    return false
  }

  async function fetchOutfit(lat, lon, isRegen = false) {
    isRegen ? setRegenerating(true) : setLoading(true)
    if (!isRegen) {
      setWeatherLoading(true)
      setWeatherUnavailable(false)
    }
    setLoadingMsgIdx(0)
    setError("")
    try {
      const data = await getOutfits(lat, lon)
      const single = (data.outfits ?? [])[0] ?? null
      setOutfit(single)
      setAdvisory(data.advisory ?? null)
      if (!applyWeatherFromOutfitsResponse(data) && lat != null && lon != null) {
        fetchWeather(lat, lon)
      } else if (!data.weather) {
        setWeatherLoading(false)
        setWeatherUnavailable(true)
      }
      // Auto-save generated outfits asynchronously
      if (data.outfits && data.outfits.length > 0) {
        const currentOutfitStr = JSON.stringify(data.outfits[0].items)
        if (lastSavedOutfitStr.current !== currentOutfitStr) {
            lastSavedOutfitStr.current = currentOutfitStr;
            Promise.all(data.outfits.map(o => saveOutfitHistory(o.items)))
                .catch(e => console.error("Failed to save outfit history", e))
        }
      }
      saveCache(user?.id, single, data.weather ?? null, data.advisory ?? null)
      if (single) {
        loadWardrobe()
        setRefinementHistory([])
        setRefinementInput("")
        setRefineError("")
      }
    } catch (err) {
      setError("Erro ao gerar outfit: " + err.message)
    } finally {
      setLoading(false)
      setRegenerating(false)
    }
  }

  async function loadWardrobe() {
    try {
      const items = await getWardrobe()
      setWardrobeItems(items)
    } catch (err) {
      console.error("Failed to load wardrobe for refine", err)
    }
  }

  function parseRefinedOutfit(data) {
    if (data.outfits?.[0]) return data.outfits[0]
    const raw = data.response?.trim()
    if (!raw) return null
    try {
      let json = raw
      if (raw.includes("```json")) {
        json = raw.split("```json")[1].split("```")[0].trim()
      } else if (raw.includes("```")) {
        json = raw.split("```")[1].split("```")[0].trim()
      } else {
        const start = raw.indexOf("[")
        const end = raw.lastIndexOf("]")
        if (start >= 0 && end > start) json = raw.slice(start, end + 1)
      }
      const parsed = JSON.parse(json)
      const arr = Array.isArray(parsed) ? parsed : [parsed]
      return arr[0] ?? null
    } catch {
      return null
    }
  }

  async function handleRefine(e) {
    e.preventDefault()
    if (!refinementInput.trim() || refining || !outfit) return

    const instruction = refinementInput.trim()
    setRefining(true)
    setRefineError("")

    try {
      const history = refinementHistory.map(text => ({ role: "user", content: text }))
      const lat = coords.current?.lat ?? null
      const lon = coords.current?.lon ?? null
      const data = await refineOutfit(
        instruction,
        history,
        {
          currentOutfit: outfit,
          wardrobeItems: wardrobeItems.length > 0 ? wardrobeItems : outfit.items,
        },
        lat,
        lon
      )
      const refined = parseRefinedOutfit(data)
      if (!refined?.items?.length) {
        throw new Error(data.response || "Não foi possível refinar o outfit.")
      }
      setOutfit(refined)
      setRefinementHistory(prev => [...prev, instruction])
      setRefinementInput("")
      saveCache(user?.id, refined, weather, advisory)
    } catch (err) {
      setRefineError(err.message || "Erro ao refinar outfit.")
    } finally {
      setRefining(false)
    }
  }

  function handleRestartRefine() {
    setRefinementHistory([])
    setRefinementInput("")
    setRefineError("")
    handleRegenerate()
  }

  function handleRegenerate() {
    clearCache()
    setRefinementHistory([])
    setRefinementInput("")
    setRefineError("")
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
          <WeatherSection
            weather={weather}
            advisory={advisory}
            loading={weatherLoading}
            unavailable={weatherUnavailable}
          />
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
          <div className="ai-pulse-ring">
            <div className="ai-core"></div>
          </div>
          <p className="loading-text" style={{ minHeight: '1.5em' }}>{LOADING_MESSAGES[loadingMsgIdx]}</p>
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

      {!loading && outfit && (
        <>
          <OutfitCard outfit={outfit} />
          <OutfitRefineBar
            input={refinementInput}
            onInputChange={setRefinementInput}
            onSubmit={handleRefine}
            refining={refining}
            error={refineError}
            history={refinementHistory}
            onRestart={handleRestartRefine}
          />
        </>
      )}
    </div>
  )
}

function WeatherSection({ weather, advisory, loading, unavailable }) {
  if (weather) {
    return <WeatherBar weather={weather} advisory={advisory} />
  }
  if (loading) {
    return (
      <div className="weather-advisory" style={{ padding: "1.5rem" }}>
        A obter clima…
      </div>
    )
  }
  if (unavailable) {
    return (
      <div className="weather-advisory" style={{ padding: "1.5rem", color: "var(--color-text-muted)" }}>
        Clima indisponível
      </div>
    )
  }
  return (
    <div className="weather-advisory" style={{ padding: "1.5rem", color: "var(--color-text-muted)" }}>
      Clima indisponível
    </div>
  )
}

function OutfitRefineBar({ input, onInputChange, onSubmit, refining, error, history, onRestart }) {
  return (
    <div className="outfit-refine-bar" style={{ marginTop: "24px" }}>
      <form onSubmit={onSubmit} className="outfit-refine-form" style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
        <input
          type="text"
          className="search-input"
          style={{ flex: "1 1 220px", minWidth: 0 }}
          placeholder="Refina o teu outfit... ex: sem verde, muda os sapatos"
          value={input}
          onChange={e => onInputChange(e.target.value)}
          disabled={refining}
        />
        <button type="submit" className="btn btn-primary" disabled={refining || !input.trim()}>
          {refining ? "A refinar…" : "Enviar"}
        </button>
      </form>

      {error && (
        <p style={{ color: "var(--color-danger, #e55)", fontSize: "0.9rem", marginTop: "8px" }}>{error}</p>
      )}

      {history.length > 0 && (
        <div className="outfit-refine-history" style={{ marginTop: "12px", display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "center" }}>
          {history.map((instr, i) => (
            <span key={i} className="filter-chip" style={{ fontSize: "0.85rem" }}>
              {instr}
            </span>
          ))}
          <button type="button" className="btn btn-secondary" style={{ fontSize: "0.85rem", padding: "6px 12px" }} onClick={onRestart} disabled={refining}>
            Recomeçar
          </button>
        </div>
      )}
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
  const containerVariants = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: {
        staggerChildren: 0.15
      }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    show: { 
      opacity: 1, 
      y: 0,
      transition: { type: "spring", stiffness: 300, damping: 24 }
    }
  };

  return (
    <motion.div 
      className="outfit-card"
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
    >
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
      <motion.div 
        className="outfit-card-items"
        variants={containerVariants}
        initial="hidden"
        animate="show"
      >
        {outfit.items.map(item => (
          <motion.div key={item.id} className="outfit-card-item" variants={itemVariants}>
            <img
              src={item.imageUrl?.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`}
              alt={item.subcategory}
              loading="lazy"
            />
            <div className="outfit-card-item-label">
                <span>{item.color} {item.subcategory}</span>
                <span className="tag tag-category" style={{marginLeft: "4px"}}>{item.category}</span>
            </div>
          </motion.div>
        ))}
      </motion.div>
    </motion.div>
  )
}
