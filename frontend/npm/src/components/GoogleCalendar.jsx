import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { useGoogleLogin } from "@react-oauth/google"

export default function GoogleCalendar() {
  const navigate = useNavigate()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [accessToken, setAccessToken] = useState(localStorage.getItem("google_access_token"))

  const login = useGoogleLogin({
    scope: "https://www.googleapis.com/auth/calendar.readonly",
    onSuccess: (tokenResponse) => {
      setAccessToken(tokenResponse.access_token)
      localStorage.setItem("google_access_token", tokenResponse.access_token)
    },
    onError: (err) => {
      console.error("Login Failed", err)
      setError("Falha na autenticação Google")
    }
  })

  useEffect(() => {
    if (accessToken) {
      fetchEvents(accessToken)
    }
  }, [accessToken])

  async function fetchEvents(token) {
    setLoading(true)
    setError(null)
    try {
      const timeMin = new Date().toISOString()
      const timeMax = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
      
      const response = await fetch(
        `https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${encodeURIComponent(timeMin)}&timeMax=${encodeURIComponent(timeMax)}&singleEvents=true&orderBy=startTime`,
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      )
      
      if (!response.ok) {
        if (response.status === 401) {
          // Token expired
          setAccessToken(null)
          localStorage.removeItem("google_access_token")
          throw new Error("Sessão expirada. Volta a iniciar sessão.")
        }
        throw new Error("Erro ao obter eventos da Google.")
      }
      
      const data = await response.json()
      // Map Google events to our format
      const formattedEvents = (data.items || []).map(item => {
        let dateObj
        if (item.start.dateTime) {
          dateObj = new Date(item.start.dateTime)
        } else if (item.start.date) {
          dateObj = new Date(item.start.date)
        }
        
        return {
          id: item.id,
          name: item.summary || "Sem Título",
          date: dateObj ? dateObj.toLocaleDateString("pt-PT") : "Sem Data",
          rawDate: dateObj
        }
      })
      
      setEvents(formattedEvents)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function handleEventClick(event) {
    let isoDate = event.date;
    if (event.rawDate) {
      const year = event.rawDate.getFullYear();
      const month = String(event.rawDate.getMonth() + 1).padStart(2, '0');
      const day = String(event.rawDate.getDate()).padStart(2, '0');
      isoDate = `${year}-${month}-${day}`;
    }
    navigate(`/events/outfits?eventName=${encodeURIComponent(event.name)}&date=${isoDate}&displayDate=${encodeURIComponent(event.date)}`)
  }

  return (
    <div className="weather-advisory" style={{ display: "flex", flexDirection: "column", height: "100%", padding: "1.2rem 1.5rem", margin: 0 }}>
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "1rem" }}>
        <span style={{ fontSize: "1.2rem" }}>📅</span>
        <h3 style={{ margin: 0, fontSize: "1rem" }}>Próximos 7 Dias</h3>
      </div>
      
      {!accessToken ? (
        <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", gap: "1rem" }}>
          <p style={{ margin: 0, fontSize: "0.9rem", color: "var(--color-text-muted)", textAlign: "center" }}>
            Conecta o teu calendário para veres os teus eventos.
          </p>
          <button 
            onClick={() => login()}
            style={{ padding: "0.6rem 1rem", background: "#4285F4", color: "white", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: "bold" }}
          >
            Conectar com Google
          </button>
        </div>
      ) : loading ? (
        <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
          A carregar...
        </div>
      ) : error ? (
        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "0.5rem" }}>
          <p style={{ margin: 0, color: "#ff4a4a", fontSize: "0.9rem" }}>{error}</p>
          <button onClick={() => login()} style={{ padding: "0.4rem 0.8rem", background: "var(--color-border)", border: "none", borderRadius: "4px", cursor: "pointer" }}>
            Tentar Novamente
          </button>
        </div>
      ) : events.length === 0 ? (
        <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <p style={{ margin: 0, color: "var(--color-text-muted)", fontSize: "0.9rem" }}>Sem eventos esta semana.</p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem", overflowY: "auto", maxHeight: "200px" }}>
          {events.map(event => (
            <div 
              key={event.id}
              onClick={() => handleEventClick(event)}
              style={{ 
                padding: "0.8rem", 
                background: "var(--color-bg)", 
                borderRadius: "8px", 
                cursor: "pointer",
                border: "1px solid var(--color-border)"
              }}
            >
              <div style={{ fontWeight: "600", fontSize: "0.95rem" }}>{event.name}</div>
              <div style={{ color: "var(--color-text-muted)", fontSize: "0.85rem", marginTop: "0.2rem" }}>
                {event.date}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
