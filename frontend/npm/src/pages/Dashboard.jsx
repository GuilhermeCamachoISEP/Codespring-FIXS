import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { getWardrobeCount } from "../services/api"

export default function Dashboard() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    const [wardrobeCount, setWardrobeCount] = useState(null)

    useEffect(() => {
        getWardrobeCount()
            .then(d => setWardrobeCount(d.count))
            .catch(() => setWardrobeCount(0))
    }, [])

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

                    {/* Weather — sprint 4 */}
                    <div className="dash-card dash-card-disabled">
                        <div className="dash-card-icon">🌤</div>
                        <div className="dash-card-body">
                            <h3>Clima hoje</h3>
                            <p>Em breve — Sprint 4</p>
                        </div>
                        <span className="dash-badge">SOON</span>
                    </div>
                </div>
            </div>
        </div>
    )
}
