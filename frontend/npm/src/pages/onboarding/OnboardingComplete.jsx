import { useNavigate } from "react-router-dom"
import { useAuth } from "../../context/AuthContext"

export default function OnboardingComplete() {
    const navigate = useNavigate()
    const { user } = useAuth()

    return (
        <div className="complete-container">
            <div className="complete-card">
                <div className="complete-icon">✓</div>
                <h1>Perfeito, {user?.name || "Stylist"}!</h1>
                <p>O teu perfil de estilo está criado.</p>
                <p className="complete-sub">
                    O teu AI Stylist já conhece o teu gosto e está pronto para
                    sugerir outfits personalizados.
                </p>
                <button className="btn-primary" onClick={() => navigate("/dashboard")}>
                    Ver o meu stylist
                </button>
            </div>
        </div>
    )
}
