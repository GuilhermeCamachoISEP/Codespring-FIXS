import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { getWardrobe, deleteWardrobeItem } from "../../services/api"

const FILTERS = [
    { id: null,          label: "Tudo" },
    { id: "tops",        label: "Tops" },
    { id: "bottoms",     label: "Calças" },
    { id: "shoes",       label: "Sapatos" },
    { id: "jackets",     label: "Casacos" },
    { id: "accessories", label: "Acessórios" },
]

export default function WardrobeGallery() {
    const [items, setItems] = useState([])
    const [filter, setFilter] = useState(null)
    const [loading, setLoading] = useState(true)
    const [selected, setSelected] = useState(null)
    const navigate = useNavigate()

    useEffect(() => {
        load()
    }, [filter])

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

    async function handleDelete(id) {
        try {
            await deleteWardrobeItem(id)
            setItems(prev => prev.filter(i => i.id !== id))
            if (selected?.id === id) setSelected(null)
        } catch (err) {
            console.error(err)
        }
    }

    function parseTags(json) {
        try { return JSON.parse(json) } catch { return [] }
    }

    return (
        <div className="wardrobe-container">
            <div className="wardrobe-topbar">
                <button className="btn-ghost" onClick={() => navigate("/dashboard")}>← Dashboard</button>
                <h1>O meu armário</h1>
                <button className="btn-primary btn-small" onClick={() => navigate("/wardrobe/upload")}>
                    + Adicionar
                </button>
            </div>

            <div className="filter-bar">
                {FILTERS.map(f => (
                    <button
                        key={String(f.id)}
                        className={`filter-btn ${filter === f.id ? "active" : ""}`}
                        onClick={() => setFilter(f.id)}
                    >
                        {f.label}
                    </button>
                ))}
            </div>

            {loading && (
                <div className="loading-state">A carregar armário...</div>
            )}

            {!loading && items.length === 0 && (
                <div className="empty-state">
                    <div style={{ fontSize: "3rem" }}>👔</div>
                    <p>Armário vazio</p>
                    <button className="btn-primary" style={{ width: "auto", marginTop: "1rem" }}
                        onClick={() => navigate("/wardrobe/upload")}>
                        Adicionar primeira peça
                    </button>
                </div>
            )}

            <div className="wardrobe-grid">
                {items.map(item => (
                    <div
                        key={item.id}
                        className={`wardrobe-item ${selected?.id === item.id ? "selected" : ""}`}
                        onClick={() => setSelected(selected?.id === item.id ? null : item)}
                    >
                        <div className="wardrobe-img-wrap">
                            <img
                                src={item.imageUrl?.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`}
                                alt={item.subcategory}
                                loading="lazy"
                            />
                        </div>
                        <div className="wardrobe-item-info">
                            <span className="wardrobe-item-name">
                                {item.color} {item.subcategory}
                            </span>
                            <div className="item-tags">
                                <span className="tag tag-category">{item.category}</span>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Detail panel */}
            {selected && (
                <div className="detail-overlay" onClick={() => setSelected(null)}>
                    <div className="detail-panel" onClick={e => e.stopPropagation()}>
                        <button className="detail-close" onClick={() => setSelected(null)}>✕</button>
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
                                    {selected.brand && selected.brand !== "unknown" &&
                                        <tr><td>Marca</td><td>{selected.brand}</td></tr>}
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
                            <button
                                className="btn-delete"
                                onClick={() => handleDelete(selected.id)}
                            >
                                Remover peça
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
