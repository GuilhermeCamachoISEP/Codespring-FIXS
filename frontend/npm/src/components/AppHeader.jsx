import { useState, useEffect } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { Sun, Moon, User, Hanger, Settings, Sparkles, Suitcase } from "./Icons"

// Bottom nav items — shown on mobile instead of top nav
const NAV_ITEMS = [
  { path: "/wardrobe",  label: "Armário",     icon: <Hanger /> },
  { path: "/outfits",   label: "Outfits",     icon: <Sparkles /> },
  { path: "/lookbook",  label: "Histórico",   icon: <BookIcon /> },
  { path: "/packing",   label: "Mala",        icon: <Suitcase /> },
  { path: "/preferences", label: "Estilo",    icon: <Settings /> },
]

function BookIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
      <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
    </svg>
  )
}

export default function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [dark, setDark] = useState(() => {
    const saved = localStorage.getItem("darkMode")
    return saved !== null ? JSON.parse(saved) : true
  })

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", dark ? "dark" : "light")
    localStorage.setItem("darkMode", JSON.stringify(dark))
  }, [dark])

  function handleLogout() {
    logout()
    navigate("/login")
  }

  const is = (path) => location.pathname === path || location.pathname.startsWith(path + "/")

  return (
    <>
      <header className="header">
        <div className="logo" onClick={() => navigate(user ? "/outfits" : "/")}>
          gaveta.
        </div>

        {/* Desktop nav — hidden on mobile via CSS */}
        <nav className="nav desktop-nav">
          {user ? (
            <>
              <button className={`nav-link ${is("/wardrobe") ? "active" : ""}`} onClick={() => navigate("/wardrobe")}>
                <Hanger /> Armário
              </button>
              <button className={`nav-link ${is("/outfits") ? "active" : ""}`} onClick={() => navigate("/outfits")}>
                <Sparkles /> Outfits
              </button>
              <button className={`nav-link ${is("/lookbook") ? "active" : ""}`} onClick={() => navigate("/lookbook")}>
                <BookIcon /> Histórico
              </button>
              <button className={`nav-link ${is("/preferences") ? "active" : ""}`} onClick={() => navigate("/preferences")}>
                <Settings /> Preferências
              </button>
              <button className={`nav-link ${is("/packing") ? "active" : ""}`} onClick={() => navigate("/packing")}>
                <Suitcase /> Mala
              </button>
            </>
          ) : (
            <>
              <button className={`nav-link ${is("/login") ? "active" : ""}`} onClick={() => navigate("/login")}>
                Entrar
              </button>
              <button className={`nav-link ${is("/register") ? "active" : ""}`} onClick={() => navigate("/register")}>
                Registar
              </button>
            </>
          )}

          <button
            className="icon-btn"
            onClick={() => setDark(d => !d)}
            aria-label={dark ? "Tema claro" : "Tema escuro"}
          >
            {dark ? <Sun /> : <Moon />}
          </button>

          {user && (
            <button
              className={`icon-btn ${is("/profile") ? "active" : ""}`}
              onClick={() => navigate("/profile")}
              aria-label="Perfil"
            >
              <User />
            </button>
          )}
        </nav>

        {/* Mobile header right — only theme + profile */}
        <div className="mobile-header-actions">
          <button className="icon-btn" onClick={() => setDark(d => !d)} aria-label="Tema">
            {dark ? <Sun /> : <Moon />}
          </button>
          {user && (
            <button className={`icon-btn ${is("/profile") ? "active" : ""}`} onClick={() => navigate("/profile")} aria-label="Perfil">
              <User />
            </button>
          )}
        </div>
      </header>

      {/* Mobile bottom navigation bar */}
      {user && (
        <nav className="mobile-bottom-nav">
          {NAV_ITEMS.map(item => (
            <button
              key={item.path}
              className={`mobile-nav-item ${is(item.path) ? "active" : ""}`}
              onClick={() => navigate(item.path)}
            >
              <span className="mobile-nav-icon">{item.icon}</span>
              <span className="mobile-nav-label">{item.label}</span>
            </button>
          ))}
        </nav>
      )}
    </>
  )
}
