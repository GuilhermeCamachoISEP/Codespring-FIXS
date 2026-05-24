import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { getOutfitHistory, getLikedOutfits, getReservedOutfits, toggleOutfitLike } from "../services/api"
import AppHeader from "../components/AppHeader"
import "./Lookbook.css"

function WornBadge() {
    return (
        <span style={{
            display: "inline-flex", alignItems: "center", gap: "4px",
            background: "var(--color-success, #22c55e)", color: "#000",
            fontSize: "0.7rem", fontWeight: 700, padding: "2px 8px",
            borderRadius: "999px", letterSpacing: "0.03em"
        }}>
            ✓ Usado
        </span>
    )
}

function HeartIcon({ isLiked }) {
    return (
        <svg 
            xmlns="http://www.w3.org/2000/svg" 
            viewBox="0 0 24 24" 
            fill={isLiked ? "currentColor" : "none"} 
            stroke="currentColor" 
            strokeWidth="2" 
            strokeLinecap="round" 
            strokeLinejoin="round" 
            width="20" height="20"
        >
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
        </svg>
    )
}

function parseDate(dateString) {
    if (!dateString) return new Date()
    // LocalDate comes as "2026-05-24" — add noon to avoid UTC midnight timezone shift
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
        return new Date(dateString + "T12:00:00")
    }
    return new Date(dateString)
}

function formatDate(dateString) {
    const date = parseDate(dateString)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)

    if (date.toDateString() === today.toDateString()) {
        return "Hoje"
    } else if (date.toDateString() === yesterday.toDateString()) {
        return "Ontem"
    } else {
        const days = ["Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"]
        return `${days[date.getDay()]}, ${date.getDate()} ${date.toLocaleString('pt-PT', { month: 'short' })}`
    }
}

export default function Lookbook() {
    const [history, setHistory] = useState([])
    const [liked, setLiked] = useState([])
    const [reserved, setReserved] = useState([])
    const [tab, setTab] = useState("all") // "all" | "worn" | "liked" | "reserved"
    const [loading, setLoading] = useState(true)
    const navigate = useNavigate()

    useEffect(() => {
        loadData()
    }, [])

    async function loadData() {
        setLoading(true)
        try {
            const [histData, likedData, reservedData] = await Promise.all([
                getOutfitHistory(),
                getLikedOutfits(),
                getReservedOutfits()
            ])
            setHistory(histData)
            setLiked(likedData)
            setReserved(reservedData)
        } catch (err) {
            console.error("Failed to load lookbook", err)
        } finally {
            setLoading(false)
        }
    }

    async function handleToggleLike(id) {
        // Optimistic UI update
        const updatedHistory = history.map(h => h.id === id ? { ...h, isLiked: !h.isLiked } : h)
        setHistory(updatedHistory)
        
        const toggledItem = updatedHistory.find(h => h.id === id)
        if (toggledItem.isLiked) {
            setLiked([toggledItem, ...liked].sort((a, b) => new Date(b.wornAt) - new Date(a.wornAt)))
        } else {
            setLiked(liked.filter(h => h.id !== id))
        }

        try {
            await toggleOutfitLike(id)
        } catch (err) {
            console.error("Failed to toggle like", err)
            // Revert on failure
            loadData()
        }
    }

    function deduplicate(list) {
        const seen = new Set();
        return list.filter(h => {
            // Usa os itens como chave para remover duplos no mesmo dia
            const day = h.wornAt.split('T')[0];
            const key = `${day}_${h.outfitItems}`;
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
        });
    }

    const wornOutfits = history.filter(h => h.worn)
    const displayedOutfits = deduplicate(
        tab === "all"      ? history :
        tab === "worn"     ? wornOutfits :
        tab === "reserved" ? reserved :
        /* liked */          liked
    )

    return (
        <div className="app-container">
            <AppHeader />

            <div className="wardrobe-top-row">
                <div className="page-header" style={{ margin: 0 }}>
                    <h1 className="page-title">Histórico</h1>
                    <p className="page-subtitle">
                        {wornOutfits.length > 0
                            ? `${wornOutfits.length} outfit${wornOutfits.length !== 1 ? "s" : ""} usado${wornOutfits.length !== 1 ? "s" : ""} · ${history.length} gerado${history.length !== 1 ? "s" : ""}`
                            : `${history.length} outfit${history.length !== 1 ? "s" : ""} gerado${history.length !== 1 ? "s" : ""}`
                        }
                    </p>
                </div>
            </div>

            <div className="lookbook-tabs">
                <button
                    className={`lookbook-tab ${tab === "all" ? "active" : ""}`}
                    onClick={() => setTab("all")}
                >
                    Todos ({history.length})
                </button>
                <button
                    className={`lookbook-tab ${tab === "worn" ? "active" : ""}`}
                    onClick={() => setTab("worn")}
                >
                    👕 Usados ({wornOutfits.length})
                </button>
                <button
                    className={`lookbook-tab ${tab === "liked" ? "active" : ""}`}
                    onClick={() => setTab("liked")}
                >
                    ♥ Favoritos ({liked.length})
                </button>
                <button
                    className={`lookbook-tab ${tab === "reserved" ? "active" : ""}`}
                    onClick={() => setTab("reserved")}
                >
                    📅 Reservados ({reserved.length})
                </button>
            </div>

            {loading && <div className="loading-state">A carregar o teu histórico...</div>}

            {!loading && displayedOutfits.length === 0 && (
                <div className="empty-state">
                    <div style={{ fontSize: "3rem", marginBottom: "1rem" }}>
                        {tab === "worn" ? "👕" : "📖"}
                    </div>
                    {tab === "all" && (
                        <>
                            <p style={{ marginBottom: "0.5rem" }}>O teu histórico está vazio.</p>
                            <p style={{ fontSize: "0.9rem" }}>Gera o teu primeiro outfit para começares a monitorizar.</p>
                            <button className="btn btn-primary" style={{ marginTop: "1rem" }} onClick={() => navigate("/outfits")}>
                                Gerar Outfit
                            </button>
                        </>
                    )}
                    {tab === "worn" && (
                        <>
                            <p style={{ marginBottom: "0.5rem" }}>Ainda não marcaste nenhum outfit como usado.</p>
                            <p style={{ fontSize: "0.9rem" }}>Na página de Outfits, clica em "Vesti este outfit hoje" para registar o que usaste.</p>
                            <button className="btn btn-primary" style={{ marginTop: "1rem" }} onClick={() => navigate("/outfits")}>
                                Ver Outfit de Hoje
                            </button>
                        </>
                    )}
                    {tab === "liked" && (
                        <p>Ainda não tens favoritos. Coloca gosto num outfit para o guardares aqui.</p>
                    )}
                    {tab === "reserved" && (
                        <>
                            <p style={{ marginBottom: "0.5rem" }}>Ainda não tens outfits reservados para eventos.</p>
                            <p style={{ fontSize: "0.9rem" }}>No calendário, clica num evento e reserva o outfit para o ver aqui.</p>
                        </>
                    )}
                </div>
            )}

            {!loading && displayedOutfits.length > 0 && (
                <div className="lookbook-grid">
                    {displayedOutfits.map(outfit => {
                        let items = []
                        try {
                            items = typeof outfit.outfitItems === 'string' ? JSON.parse(outfit.outfitItems) : outfit.outfitItems
                        } catch (e) {
                            console.error("Failed to parse items", e)
                        }

                        return (
                            <div key={outfit.id} className={`lookbook-card ${outfit.isLiked ? 'liked-glow' : ''} ${outfit.worn ? 'worn-card' : outfit.eventName ? 'reserved-card' : 'generated-card'}`}>
                                <div className="lookbook-card-header">
                                    <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                                        <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                                            <span className="lookbook-date">
                                                {outfit.worn && outfit.wornDate
                                                    ? formatDate(outfit.wornDate)
                                                    : formatDate(outfit.wornAt)}
                                            </span>
                                            {outfit.worn ? <WornBadge /> : outfit.eventName ? (
                                                <span style={{
                                                    display: "inline-flex", alignItems: "center", gap: "4px",
                                                    background: "var(--color-primary, #7c3aed)", color: "#fff",
                                                    fontSize: "0.7rem", fontWeight: 700, padding: "2px 8px",
                                                    borderRadius: "999px", letterSpacing: "0.03em"
                                                }}>
                                                    📅 {outfit.eventName}
                                                </span>
                                            ) : (
                                                <span style={{
                                                    fontSize: "0.7rem", color: "var(--color-text-muted)",
                                                    fontStyle: "italic"
                                                }}>gerado</span>
                                            )}
                                        </div>
                                    </div>
                                    <button
                                        className={`like-btn ${outfit.isLiked ? 'liked' : ''}`}
                                        onClick={() => handleToggleLike(outfit.id)}
                                        aria-label="Toggle like"
                                    >
                                        <HeartIcon isLiked={outfit.isLiked} />
                                    </button>
                                </div>
                                <div className="lookbook-items-row">
                                    {items.map(item => (
                                        <div key={item.id} className="lookbook-item">
                                            <div className="item-img-container">
                                                {item.imageUrl ? (
                                                    <img src={item.imageUrl?.startsWith("http") ? item.imageUrl : `http://localhost:8080${item.imageUrl}`} alt={item.subcategory} className="item-img" />
                                                ) : (
                                                    <div className="item-placeholder">👕</div>
                                                )}
                                            </div>
                                            <div className="item-details">
                                                <span className="item-category">{item.category}</span>
                                                <span className="item-name">
                                                    {item.color && item.color !== 'unknown' && (
                                                        <span 
                                                            className="item-color-indicator" 
                                                            style={{ 
                                                                background: item.color === 'white' || item.color === 'branco' ? '#fff' : 
                                                                            item.color === 'black' || item.color === 'preto' ? '#000' : 
                                                                            item.color 
                                                            }}
                                                            title={item.color}
                                                        ></span>
                                                    )}
                                                    {item.subcategory}
                                                </span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )
                    })}
                </div>
            )}
        </div>
    )
}
