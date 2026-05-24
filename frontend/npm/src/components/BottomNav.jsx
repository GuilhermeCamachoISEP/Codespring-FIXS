import { useNavigate, useLocation } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import { Hanger, Sparkles, Images, Settings, User } from "./Icons"

const NAV_ITEMS = [
  { path: "/outfits",     Icon: Sparkles, label: "Outfits"   },
  { path: "/wardrobe",    Icon: Hanger,   label: "Armário"   },
  { path: "/inspiration", Icon: Images,   label: "Looks"     },
  { path: "/preferences", Icon: Settings, label: "Estilo"    },
  { path: "/profile",     Icon: User,     label: "Perfil"    },
]

export default function BottomNav() {
  const { user } = useAuth()
  const navigate  = useNavigate()
  const location  = useLocation()

  if (!user) return null

  const is = (path) =>
    location.pathname === path || location.pathname.startsWith(path + "/")

  return (
    <nav className="bottom-nav">
      {NAV_ITEMS.map(({ path, Icon, label }) => (
        <button
          key={path}
          className={`bottom-nav-item${is(path) ? " active" : ""}`}
          onClick={() => navigate(path)}
        >
          <Icon />
          <span>{label}</span>
        </button>
      ))}
    </nav>
  )
}
