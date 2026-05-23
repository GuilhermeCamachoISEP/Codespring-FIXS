import { useState, useEffect, useRef } from "react"
import { useNavigate } from "react-router-dom"
import { getOutfits } from "../services/api"

const TEMP_LABELS = {
    "very-cold": "Muito frio",
    "cold": "Frio",
    "cool": "Fresco",
    "mild": "Ameno",
    "warm": "Quente",
    "hot": "Muito quente",
}

export default function OutfitsPage() {
    const [outfits, setOutfits] = useState([])
    const [weather, setWeather] = useState(null)
    const [advisory, setAdvisory] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState("")
    const coords = useRef(null)
    const navigate = useNavigate()

    useEffect(() => {
        if (!navigator.geolocation) {
            loadData(null, null)
            return
        }
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                coords.current = { lat: pos.coords.latitude, lon: pos.coords.longitude }
                loadData(coords.current.lat, coords.current.lon)
            },
            () => loadData(null, null),
            { timeout: 6000 }
        )
    }, [])

    async function loadData(lat, lon) {
        setLoading(true)
        setError("")
        try {
            const data = await getOutfits(lat, lon)
            setOutfits(data.outfits ?? [])
            setWeather(data.weather ?? null)
            setAdvisory(data.advisory ?? null)
        } catch (err) {
            setError("Erro ao gerar outfits: " + err.message)
        } finally {
            setLoading(false)
        }
    }

    function reload() {
        loadData(coords.current?.lat ?? null, coords.current?.lon ?? null)
    }

    return (
        <div className="wardrobe-container">
            <div className="wardrobe-topbar">
                <button className="btn-ghost" onClick={() => navigate("/dashboard")}>← Dashboard</button>
                <h1>Os meus outfits</h1>
                <button className="btn-outline btn-small" onClick={reload} disabled={loading}>
                    {loading ? "..." : "↺ Gerar novos"}
                </button>
            </div>

            {weather && <WeatherAdvisoryPanel weather={weather} advisory={advisory} />}

            {loading && (
                <div className="loading-state">
                    <div style={{ fontSize: "2rem", marginBottom: "0.5rem" }}>✨</div>
                    <p>
                        O Gemini AI está a combinar as tuas peças
                        {weather ? ` para ${Math.round(weather.temperature)}°C em ${weather.city}` : ""}...
                    </p>
                </div>
            )}

            {error && (
                <div className="empty-state">
                    <div style={{ fontSize: "2rem" }}>⚠️</div>
                    <p>{error}</p>
                    <button className="btn-primary" style={{ width: "auto", marginTop: "1rem" }} onClick={reload}>
                        Tentar novamente
                    </button>
                </div>
            )}

            {!loading && !error && outfits.length === 0 && (
                <div className="empty-state">
                    <div style={{ fontSize: "3rem" }}>👔</div>
                    <p>Ainda não há outfits gerados.</p>
                    <p style={{ fontSize: "0.9rem", color: "#888" }}>Adiciona peças ao teu armário primeiro.</p>
                    <button
                        className="btn-primary"
                        style={{ width: "auto", marginTop: "1rem" }}
                        onClick={() => navigate("/wardrobe/upload")}
                    >
                        Adicionar roupa
                    </button>
                </div>
            )}

            <div className="outfits-grid">
                {outfits.map((outfit, i) => (
                    <OutfitCard key={i} outfit={outfit} />
                ))}
            </div>
        </div>
    )
}

function WeatherAdvisoryPanel({ weather, advisory }) {
    return (
        <div className="weather-advisory">
            <div className="weather-advisory-header">
                <div className="weather-icon-large">{weather.icon}</div>
                <div className="weather-info">
                    <div className="weather-temp">{Math.round(weather.temperature)}°C</div>
                    <div className="weather-meta">
                        <span className="weather-desc">{weather.description}</span>
                        {weather.windSpeed > 20 && (
                            <span className="weather-wind"> · {Math.round(weather.windSpeed)} km/h</span>
                        )}
                    </div>
                    <div className="weather-city">📍 {weather.city}</div>
                </div>
                <span className={`weather-badge weather-badge-${weather.tempCategory}`}>
                    {TEMP_LABELS[weather.tempCategory] ?? "Variável"}
                </span>
            </div>

            {advisory?.tips?.length > 0 && (
                <div className="weather-tips">
                    <div className="weather-tips-label">Dicas para hoje</div>
                    {advisory.tips.map((tip, i) => (
                        <div key={i} className="weather-tip">{tip}</div>
                    ))}
                </div>
            )}

            {advisory?.wardrobeAlert && (
                <div className="weather-alert">
                    <span>⚠️</span>
                    <span>{advisory.wardrobeAlert}</span>
                </div>
            )}
        </div>
    )
}

function OutfitCard({ outfit }) {
    return (
        <div className="outfit-card">
            <div className="outfit-header">
                <h3 className="outfit-name">{outfit.name}</h3>
                <p className="outfit-desc">{outfit.description}</p>
                {outfit.weatherNote && (
                    <div className="outfit-weather-note">
                        <span>🌡️</span>
                        <span>{outfit.weatherNote}</span>
                    </div>
                )}
            </div>
            <div className="outfit-items">
                {outfit.items.map(item => (
                    <div key={item.id} className="outfit-item">
                        <img src={`http://localhost:8080${item.imageUrl}`} alt={item.subcategory} />
                        <div className="outfit-item-label">
                            <span>{item.color} {item.subcategory}</span>
                            <span className="tag tag-category">{item.category}</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}
