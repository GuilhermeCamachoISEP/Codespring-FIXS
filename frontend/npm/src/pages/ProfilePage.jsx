import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { getWardrobeCount } from "../services/api"
import AppHeader from "../components/AppHeader"
import { ArrowLeft, LogOut } from "../components/Icons"

export default function ProfilePage() {
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

  const memberSince = (() => {
    const stored = localStorage.getItem("user")
    if (!stored) return "—"
    return new Date().toLocaleDateString("pt-PT", { month: "long", year: "numeric" })
  })()

  return (
    <div className="app-container">
      <AppHeader />

      <button className="back-link" onClick={() => navigate(-1)}>
        <ArrowLeft /> Voltar
      </button>

      <div className="page-header">
        <h1 className="page-title">Perfil</h1>
        <p className="page-subtitle">A tua conta e definições</p>
      </div>

      <div className="settings-section">
        <h3>Conta</h3>
        <div className="setting-row">
          <span className="setting-label">Nome</span>
          <span className="setting-value">{user?.name ?? "—"}</span>
        </div>
        <div className="setting-row">
          <span className="setting-label">Email</span>
          <span className="setting-value">{user?.email ?? "—"}</span>
        </div>
        <div className="setting-row">
          <span className="setting-label">Membro desde</span>
          <span className="setting-value">{memberSince}</span>
        </div>
      </div>

      <div className="settings-section">
        <h3>Estatísticas</h3>
        <div className="setting-row">
          <span className="setting-label">Peças no armário</span>
          <span className="setting-value">
            {wardrobeCount === null ? "…" : wardrobeCount}
          </span>
        </div>
      </div>

      <div className="settings-section">
        <h3>Ações</h3>
        <div className="setting-row">
          <span className="setting-label">Preferências de estilo</span>
          <button className="btn btn-secondary" onClick={() => navigate("/preferences")}>
            Editar
          </button>
        </div>
        <div className="setting-row">
          <span className="setting-label">Terminar sessão</span>
          <button
            className="btn btn-secondary"
            onClick={handleLogout}
            style={{ display: "flex", alignItems: "center", gap: "6px" }}
          >
            <LogOut style={{ width: 16, height: 16 }} />
            Sair
          </button>
        </div>
      </div>
    </div>
  )
}
