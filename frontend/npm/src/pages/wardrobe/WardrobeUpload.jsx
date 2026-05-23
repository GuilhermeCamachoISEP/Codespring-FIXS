import { useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
import { uploadClothingItem } from "../../services/api"

const CATEGORIES = [
    { id: "tops", label: "Tops", icon: "TS", hint: "t-shirts, hoodies, camisas, sweatshirts" },
    { id: "bottoms", label: "Calcas", icon: "JE", hint: "jeans, cargos, shorts, calcas" },
    { id: "shoes", label: "Sapatos", icon: "SN", hint: "sneakers, botas, loafers" },
    { id: "jackets", label: "Casacos", icon: "JK", hint: "bomber, trench, casaco, hoodie zip" },
    { id: "accessories", label: "Acessorios", icon: "AC", hint: "cintos, chapeus, carteiras, joias" },
]

function parseTags(tagsJson) {
    try {
        return JSON.parse(tagsJson || "[]")
    } catch {
        return []
    }
}

export default function WardrobeUpload() {
    const [activeCategory, setActiveCategory] = useState("tops")
    const [queue, setQueue] = useState([])
    const [uploading, setUploading] = useState(false)
    const [results, setResults] = useState([])
    const [error, setError] = useState("")
    const fileInputRef = useRef(null)
    const navigate = useNavigate()

    function handleFileSelect(e) {
        const files = Array.from(e.target.files)
        const previews = files.map(file => ({
            file,
            url: URL.createObjectURL(file),
            category: activeCategory,
        }))
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
        const failed = []

        for (const item of queue) {
            try {
                const result = await uploadClothingItem(item.file, item.category)
                setResults(prev => [result, ...prev])
            } catch (err) {
                failed.push(item)
                setError(err?.message || "A AI não conseguiu classificar uma ou mais peças. Confirma a GROQ_API_KEY no backend.")
            }
        }
        setQueue(failed)
        setUploading(false)
    }

    return (
        <div className="wardrobe-upload-container">
            <div className="wardrobe-header">
                <button className="btn-ghost" onClick={() => navigate("/wardrobe")}>Voltar ao armario</button>
                <h1>Adicionar roupa</h1>
                <p>A AI identifica cada peca automaticamente e usa a categoria como pista.</p>
            </div>

            <div className="category-tabs">
                {CATEGORIES.map(cat => (
                    <button
                        key={cat.id}
                        className={`category-tab ${activeCategory === cat.id ? "active" : ""}`}
                        onClick={() => setActiveCategory(cat.id)}
                    >
                        <span>{cat.icon}</span>
                        <span>{cat.label}</span>
                    </button>
                ))}
            </div>

            <p className="category-hint">{CATEGORIES.find(c => c.id === activeCategory)?.hint}</p>

            <div className="drop-zone" onClick={() => fileInputRef.current?.click()}>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    multiple
                    style={{ display: "none" }}
                    onChange={handleFileSelect}
                />
                <div className="drop-icon">IMG</div>
                <p>Clica para adicionar fotos</p>
                <p className="drop-sub">JPG, PNG, WEBP ate 10MB</p>
            </div>

            {queue.length > 0 && (
                <div className="upload-section">
                    <div className="section-title">
                        <span>Prontos para enviar ({queue.length})</span>
                        {!uploading && (
                            <button className="btn-primary btn-upload-all" onClick={uploadAll}>
                                Enviar e classificar com AI
                            </button>
                        )}
                        {uploading && <span className="uploading-text">A classificar com AI...</span>}
                    </div>
                    <div className="item-grid">
                        {queue.map((item, i) => (
                            <div key={i} className="item-preview">
                                <img src={item.url} alt="preview" />
                                <span className="item-category-pill">
                                    {CATEGORIES.find(c => c.id === item.category)?.label}
                                </span>
                                {!uploading && (
                                    <button className="item-remove" onClick={() => removeFromQueue(i)}>x</button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {error && <p className="error">{error}</p>}

            {results.length > 0 && (
                <div className="upload-section">
                    <div className="section-title">
                        <span>Classificados por AI ({results.length})</span>
                        <button className="btn-outline" onClick={() => navigate("/wardrobe")}>
                            Ver armario
                        </button>
                    </div>
                    <div className="item-grid">
                        {results.map(item => (
                            <div key={item.id} className="item-card classified">
                                <img src={`http://localhost:8080${item.imageUrl}`} alt={item.subcategory} />
                                <div className="item-meta">
                                    <span className="item-name">{item.color} {item.subcategory}</span>
                                    <div className="item-tags">
                                        {item.category && <span className="tag tag-category">{item.category}</span>}
                                        {item.fit && <span className="tag tag-fit">{item.fit}</span>}
                                        {parseTags(item.styleTags).slice(0, 2).map(tag => (
                                            <span key={tag} className="tag">{tag}</span>
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
