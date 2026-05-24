import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { getWardrobe, deleteWardrobeItem } from "../../services/api"
import AppHeader from "../../components/AppHeader"
import { Plus, Search, X } from "../../components/Icons"

const FILTERS = [
  { id: null, label: "Tudo" },
  { id: "tops", label: "Tops" },
  { id: "bottoms", label: "Calças" },
  { id: "shoes", label: "Sapatos" },
  { id: "jackets", label: "Casacos" },
  { id: "accessories", label: "Acessórios" },
]

export default function WardrobeGallery() {
  const [items, setItems] = useState([])
  const [filter, setFilter] = useState(null)
  const [search, setSearch] = useState("")
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState(null)
  const navigate = useNavigate()

  useEffect(() => { load() }, [filter])

  async function load() {
    setLoading(true)
    try {
      const data = await getWardrobe(filter)
      setItems(data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  async function handleDelete(id, force = false) {
    try {
      await deleteWardrobeItem(id, force)
      setItems(prev => prev.filter(i => i.id !== id))
      if (selected?.id === id) setSelected(null)
    } catch (err) {
      if (err.message === "RESERVED") {
          const confirmDelete = window.confirm("⚠️ Esta peça está reservada num dos teus outfits/eventos planeados!\n\nQueres mesmo apagá-la? (O outfit será afetado)")
          if (confirmDelete) {
              handleDelete(id, true)
          }
      } else {
          console.error(err)
      }
    }
  }

  function parseTags(json) {
    try { return JSON.parse(json) } catch { return [] }
  }

  const visible = search.trim()
    ? items.filter(i =>
        `${i.color ?? ""} ${i.subcategory ?? ""} ${i.category ?? ""}`
          .toLowerCase()
          .includes(search.toLowerCase())
      )
    : items

  return (
    <div className="app-container">
      <AppHeader />

      <div className="wardrobe-top-row">
        <div className="page-header" style={{ margin: 0 }}>
          <h1 className="page-title">O meu armário</h1>
          <p className="page-subtitle">{items.length} peça{items.length !== 1 ? "s" : ""}</p>
        </div>

        <div style={{ display: "flex", gap: "10px", alignItems: "center", flexWrap: "wrap" }}>
          <div className="search-wrapper">
            <Search className="search-icon" />
            <input
              className="search-input"
              placeholder="Pesquisar…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <button className="btn btn-primary" onClick={() => navigate("/wardrobe/upload")}>
            <Plus /> Add Foto
          </button>
          <button className="btn btn-secondary" style={{ backgroundColor: 'var(--accent)', color: '#000', border: 'none', display: 'flex', alignItems: 'center', gap: '8px' }} onClick={() => navigate("/chat")}>
            <span>💬</span> Adição Rápida (sem fotos)
          </button>
        </div>
      </div>

      <div className="wardrobe-filters">
        {FILTERS.map(f => (
          <button
            key={String(f.id)}
            className={`filter-chip ${filter === f.id ? "active" : ""}`}
            onClick={() => setFilter(f.id)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading && <div className="loading-state">A carregar armário…</div>}

      {!loading && visible.length === 0 && (
        <div className="empty-state">
          <div style={{ fontSize: "3rem" }}>👔</div>
          <p style={{ marginBottom: "0.5rem" }}>{search ? "Nenhuma peça encontrada." : "Armário vazio."}</p>
          {!search && (
            <button className="btn btn-primary" style={{ marginTop: "1rem" }} onClick={() => navigate("/wardrobe/upload")}>
              Adicionar primeira peça
            </button>
          )}
        </div>
      )}

      {!loading && visible.length > 0 && (
        <div className="wardrobe-grid">
          <div className="wardrobe-item add-new" onClick={() => navigate("/wardrobe/upload")}>
            <Plus />
            <span>Adicionar</span>
          </div>

          {visible.map(item => (
            <div
              key={item.id}
              className={`wardrobe-item ${selected?.id === item.id ? "selected" : ""}`}
              onClick={() => setSelected(selected?.id === item.id ? null : item)}
            >
              <img
                src={item.imageUrl?.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`}
                alt={item.subcategory}
                loading="lazy"
              />
              <div className="wardrobe-item-label">
                <span className="wardrobe-item-name">{item.color} {item.subcategory}</span>
                <span className="wardrobe-item-cat">{item.category}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {selected && (
        <div className="detail-overlay" onClick={() => setSelected(null)}>
          <div className="detail-panel" onClick={e => e.stopPropagation()}>
            <button className="detail-close" onClick={() => setSelected(null)}>
              <X style={{ width: 14, height: 14 }} />
            </button>
            <img
              src={selected.imageUrl?.startsWith("http") ? selected.imageUrl : `http://localhost:8080${selected.imageUrl}`}
              alt={selected.subcategory}
              className="detail-img"
            />
            <div className="detail-info">
              <h2>{selected.color} {selected.subcategory}</h2>
              <table className="detail-table">
                <tbody>
                  {selected.category && <tr><td>Categoria</td><td>{selected.category}</td></tr>}
                  {selected.fit && <tr><td>Fit</td><td>{selected.fit}</td></tr>}
                  {selected.material && <tr><td>Material</td><td>{selected.material}</td></tr>}
                  {selected.brand && selected.brand !== "unknown" && <tr><td>Marca</td><td>{selected.brand}</td></tr>}
                  {selected.season && (
                    <tr><td>Estação</td><td>{parseTags(selected.season).join(", ")}</td></tr>
                  )}
                </tbody>
              </table>
              <div className="item-tags" style={{ marginTop: "0.75rem" }}>
                {parseTags(selected.styleTags).map(t => (
                  <span key={t} className="tag">{t}</span>
                ))}
              </div>
              <button className="btn-delete" onClick={() => handleDelete(selected.id)}>
                Remover peça
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
