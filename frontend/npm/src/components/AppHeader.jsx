import { useState, useEffect } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { Sun, Moon, User, Hanger, Settings, Sparkles, Images, Suitcase } from "./Icons"

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
    <header className="header">
      <div className="logo" onClick={() => navigate(user ? "/outfits" : "/")}>
        Outfit AI
      </div>

      <nav className="nav">
        {user ? (
          <>
            <button
              className={`nav-link ${is("/wardrobe") ? "active" : ""}`}
              onClick={() => navigate("/wardrobe")}
            >
              <Hanger />
              Armário
            </button>
            <button
              className={`nav-link ${is("/outfits") ? "active" : ""}`}
              onClick={() => navigate("/outfits")}
            >
              <Sparkles />
              Outfits
            </button>
            <button
              className={`nav-link ${is("/inspiration") ? "active" : ""}`}
              onClick={() => navigate("/inspiration")}
            >
              <Images />
              Inspiração
            </button>
            <button
              className={`nav-link ${is("/preferences") ? "active" : ""}`}
              onClick={() => navigate("/preferences")}
            >
              <Settings />
              Preferências
            </button>
            <button
              className={`nav-link ${is("/packing") ? "active" : ""}`}
              onClick={() => navigate("/packing")}
            >
              <Suitcase />
              Mala
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
          aria-label={dark ? "Mudar para tema claro" : "Mudar para tema escuro"}
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
    </header>
  )
}
