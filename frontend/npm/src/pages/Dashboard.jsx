import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { getWardrobeCount, getWeather } from "../services/api"

const TEMP_LABELS = {
    "very-cold": "Muito frio",
    "cold": "Frio",
    "cool": "Fresco",
    "mild": "Ameno",
    "warm": "Quente",
    "hot": "Muito quente",
}

export default function Dashboard() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    const [wardrobeCount, setWardrobeCount] = useState(null)
    const [weather, setWeather] = useState(null)
    const [weatherState, setWeatherState] = useState("loading") // loading | ok | denied | error

    useEffect(() => {
        getWardrobeCount()
            .then(d => setWardrobeCount(d.count))
            .catch(() => setWardrobeCount(0))
        fetchWeather()
    }, [])

    function fetchWeather() {
        setWeatherState("loading")
        if (!navigator.geolocation) { setWeatherState("denied"); return }
        navigator.geolocation.getCurrentPosition(
            pos => {
                getWeather(pos.coords.latitude, pos.coords.longitude)
                    .then(w => { setWeather(w); setWeatherState("ok") })
                    .catch(() => setWeatherState("error"))
            },
            () => setWeatherState("denied"),
            { timeout: 6000 }
        )
    }

    function handleLogout() {
        logout()
        navigate("/login")
    }

    return (
        <div className="dashboard-container">
            <header className="dashboard-header">
                <span className="auth-logo">STYLIST AI</span>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                    <span style={{ color: "#aaa", fontSize: "0.9rem" }}>Olá, {user?.name}</span>
                    <button className="btn-outline" style={{ padding: "0.5rem 1rem" }} onClick={() => navigate("/settings/styles")}>Preferências</button>
                    <button className="btn-outline" onClick={handleLogout}>Sair</button>
                </div>
            </header>

            <div className="dashboard-content">
                <div className="dashboard-grid">
                    {/* Wardrobe card */}
                    <div className="dash-card" onClick={() => navigate("/wardrobe")}>
                        <div className="dash-card-icon">👔</div>
                        <div className="dash-card-body">
                            <h3>O meu armário</h3>
                            <p>
                                {wardrobeCount === null
                                    ? "A carregar..."
                                    : wardrobeCount === 0
                                        ? "Adiciona a tua primeira peça"
                                        : `${wardrobeCount} peça${wardrobeCount !== 1 ? "s" : ""} adicionada${wardrobeCount !== 1 ? "s" : ""}`}
                            </p>
                        </div>
                        <span className="dash-arrow">→</span>
                    </div>

                    {/* Upload card */}
                    <div className="dash-card dash-card-accent" onClick={() => navigate("/wardrobe/upload")}>
                        <div className="dash-card-icon">📷</div>
                        <div className="dash-card-body">
                            <h3>Adicionar roupa</h3>
                            <p>Gemini AI classifica automaticamente</p>
                        </div>
                        <span className="dash-arrow">→</span>
                    </div>

                    {/* Outfits card */}
                    <div className="dash-card dash-card-accent" onClick={() => navigate("/outfits")}>
                        <div className="dash-card-icon">✨</div>
                        <div className="dash-card-body">
                            <h3>Os meus outfits</h3>
                            <p>Gemini AI combina as tuas peças</p>
                        </div>
                        <span className="dash-arrow">→</span>
                    </div>

                    {/* Weather card */}
                    <div className="dash-card dash-card-weather" onClick={() => navigate("/outfits")}>
                        {weatherState === "loading" && <>
                            <div className="dash-card-icon">🌤</div>
                            <div className="dash-card-body">
                                <h3>Clima hoje</h3>
                                <p style={{ color: "#555" }}>A obter localização…</p>
                            </div>
                        </>}
                        {weatherState === "ok" && weather && <>
                            <div className="dash-weather-icon">{weather.icon}</div>
                            <div className="dash-card-body">
                                <h3 className="dash-weather-temp">{Math.round(weather.temperature)}°C <span className={`dash-weather-badge weather-badge-${weather.tempCategory}`}>{TEMP_LABELS[weather.tempCategory]}</span></h3>
                                <p>{weather.description}{weather.windSpeed > 20 ? ` · ${Math.round(weather.windSpeed)} km/h` : ""}</p>
                                <p className="dash-weather-city">📍 {weather.city}</p>
                            </div>
                            <span className="dash-arrow">→</span>
                        </>}
                        {(weatherState === "denied" || weatherState === "error") && <>
                            <div className="dash-card-icon">🌤</div>
                            <div className="dash-card-body">
                                <h3>Clima hoje</h3>
                                <p>{weatherState === "denied" ? "Localização não disponível" : "Erro ao obter clima"}</p>
                            </div>
                            <button className="dash-weather-retry" onClick={e => { e.stopPropagation(); fetchWeather() }}>↺</button>
                        </>}
                    </div>
                </div>
            </div>
        </div>
    )
}
