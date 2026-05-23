import { useNavigate } from "react-router-dom"

const MOCK_EVENTS = [
  { id: 1, name: "Casamento do João", date: getFutureDate(2), theme: "Formal / Casamento" },
  { id: 2, name: "Jantar de Empresa", date: getFutureDate(5), theme: "Business Casual" },
  { id: 3, name: "Despedida de Solteiro", date: getFutureDate(10), theme: "Festa / Casual" },
]

function getFutureDate(daysAhead) {
  const date = new Date()
  date.setDate(date.getDate() + daysAhead)
  return date.toISOString().split("T")[0]
}

export default function CalendarMock() {
  const navigate = useNavigate()

  function handleEventClick(event) {
    navigate(`/events/outfits?eventName=${encodeURIComponent(event.name)}&date=${event.date}`)
  }

  return (
    <div className="weather-advisory" style={{ display: "flex", flexDirection: "column", height: "100%", padding: "1.2rem 1.5rem", margin: 0 }}>
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "1rem" }}>
        <span style={{ fontSize: "1.2rem" }}>📅</span>
        <h3 style={{ margin: 0, fontSize: "1rem" }}>Próximos Eventos</h3>
      </div>
      
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {MOCK_EVENTS.map(ev => (
          <div 
            key={ev.id} 
            className="calendar-event-item"
            onClick={() => handleEventClick(ev)}
            style={{ 
              background: "rgba(255, 255, 255, 0.05)", 
              padding: "0.8rem", 
              borderRadius: "8px",
              cursor: "pointer",
              transition: "background 0.2s"
            }}
            onMouseOver={e => e.currentTarget.style.background = "rgba(255, 255, 255, 0.1)"}
            onMouseOut={e => e.currentTarget.style.background = "rgba(255, 255, 255, 0.05)"}
          >
            <div style={{ fontSize: "0.8rem", color: "#e1a8ff", marginBottom: "0.2rem" }}>{ev.date}</div>
            <div style={{ fontWeight: "600", fontSize: "0.95rem" }}>{ev.name}</div>
            <div style={{ fontSize: "0.8rem", color: "#aaa" }}>{ev.theme}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
