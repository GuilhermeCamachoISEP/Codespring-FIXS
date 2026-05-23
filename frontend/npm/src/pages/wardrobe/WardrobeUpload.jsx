import { useState, useRef } from "react"
import { useNavigate } from "react-router-dom"
import { uploadClothingItem } from "../../services/api"

const CATEGORIES = [
    { id: "tops",        label: "Tops",        emoji: "👕", hint: "t-shirts, hoodies, shirts, sweatshirts" },
    { id: "bottoms",     label: "Calças",       emoji: "👖", hint: "jeans, cargos, shorts, calças" },
    { id: "shoes",       label: "Sapatos",      emoji: "👟", hint: "sneakers, boots, loafers" },
    { id: "jackets",     label: "Casacos",      emoji: "🧥", hint: "bomber, trench, hoodie zip" },
    { id: "accessories", label: "Acessórios",   emoji: "🧣", hint: "cintos, chapéus, carteiras, joias" },
]

export default function WardrobeUpload() {
    const [activeCategory, setActiveCategory] = useState("tops")
    const [queue, setQueue] = useState([])         // files waiting to upload
    const [uploading, setUploading] = useState(false)
    const [results, setResults] = useState([])     // classified items
    const [error, setError] = useState("")
    const fileInputRef = useRef(null)
    const navigate = useNavigate()

    function handleFileSelect(e) {
        const files = Array.from(e.target.files)
        const previews = files.map(f => ({ file: f, url: URL.createObjectURL(f) }))
        setQueue(prev => [...prev, ...previews])
        e.target.value = ""
    }

    function removeFromQueue(index) {
        setQueue(prev => prev.filter((_, i) => i !== index))
    }

    async function uploadAll() {
        if (queue.length === 0) return
        setUploading(true)
        setError("")

        for (const item of queue) {
            try {
                const result = await uploadClothingItem(item.file)
                setResults(prev => [result, ...prev])
            } catch (err) {
                setError("Erro ao enviar uma peça: " + err.message)
            }
        }
        setQueue([])
        setUploading(false)
    }

    return (
        <div className="wardrobe-upload-container">
            <div className="wardrobe-header">
                <button className="btn-ghost" onClick={() => navigate("/wardrobe")}>← Armário</button>
                <h1>Adicionar roupa</h1>
                <p>O Claude AI vai identificar cada peça automaticamente</p>
            </div>

            {/* Category picker */}
            <div className="category-tabs">
                {CATEGORIES.map(cat => (
                    <button
                        key={cat.id}
                        className={`category-tab ${activeCategory === cat.id ? "active" : ""}`}
                        onClick={() => setActiveCategory(cat.id)}
                    >
                        <span>{cat.emoji}</span>
                        <span>{cat.label}</span>
                    </button>
                ))}
            </div>

            <p className="category-hint">
                {CATEGORIES.find(c => c.id === activeCategory)?.hint}
            </p>

            {/* Drop zone */}
            <div
                className="drop-zone"
                onClick={() => fileInputRef.current?.click()}
            >
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    multiple
                    style={{ display: "none" }}
                    onChange={handleFileSelect}
                />
                <div className="drop-icon">📷</div>
                <p>Clica para adicionar fotos</p>
                <p className="drop-sub">JPG, PNG, WEBP até 10MB</p>
            </div>

            {/* Queue preview */}
            {queue.length > 0 && (
                <div className="upload-section">
                    <div className="section-title">
                        <span>Prontos para enviar ({queue.length})</span>
                        {!uploading && (
                            <button className="btn-primary btn-upload-all" onClick={uploadAll}>
                                Enviar e classificar com AI
                            </button>
                        )}
                        {uploading && <span className="uploading-text">⏳ A classificar com Claude...</span>}
                    </div>
                    <div className="item-grid">
                        {queue.map((item, i) => (
                            <div key={i} className="item-preview">
                                <img src={item.url} alt="preview" />
                                {!uploading && (
                                    <button className="item-remove" onClick={() => removeFromQueue(i)}>✕</button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {error && <p className="error">{error}</p>}

            {/* Results */}
            {results.length > 0 && (
                <div className="upload-section">
                    <div className="section-title">
                        <span>Classificados por AI ✓ ({results.length})</span>
                        <button className="btn-outline" onClick={() => navigate("/wardrobe")}>
                            Ver armário
                        </button>
                    </div>
                    <div className="item-grid">
                        {results.map(item => (
                            <div key={item.id} className="item-card classified">
                                <img
                                    src={`http://localhost:8080${item.imageUrl}`}
                                    alt={item.subcategory}
                                />
                                <div className="item-meta">
                                    <span className="item-name">
                                        {item.color} {item.subcategory}
                                    </span>
                                    <div className="item-tags">
                                        {item.category && (
                                            <span className="tag tag-category">{item.category}</span>
                                        )}
                                        {item.fit && (
                                            <span className="tag tag-fit">{item.fit}</span>
                                        )}
                                        {item.styleTags && JSON.parse(item.styleTags).slice(0, 2).map(t => (
                                            <span key={t} className="tag">{t}</span>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}
