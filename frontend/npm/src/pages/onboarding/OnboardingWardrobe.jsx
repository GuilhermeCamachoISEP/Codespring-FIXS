import { useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
import { uploadClothingItem } from "../../services/api"

const CATEGORIES = [
    { id: "tops", label: "Tops", icon: "TS", hint: "t-shirts, hoodies, camisas" },
    { id: "bottoms", label: "Calcas", icon: "JE", hint: "jeans, cargos, shorts" },
    { id: "shoes", label: "Sapatos", icon: "SN", hint: "sneakers, botas, loafers" },
    { id: "jackets", label: "Casacos", icon: "JK", hint: "bomber, trench, hoodie zip" },
    { id: "accessories", label: "Acessorios", icon: "AC", hint: "cintos, chapeus, carteiras" },
]

export default function OnboardingWardrobe() {
    const [activeCategory, setActiveCategory] = useState("tops")
    const [queue, setQueue] = useState([])
    const [uploading, setUploading] = useState(false)
    const [uploaded, setUploaded] = useState([])
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
                setUploaded(prev => [result, ...prev])
            } catch (err) {
                failed.push(item)
                setError("A AI nao conseguiu classificar uma ou mais pecas. Confirma a GEMINI_API_KEY no backend.")
            }
        }
        setQueue(failed)
        setUploading(false)
    }

    return (
        <div className="onboarding-container">
            <div className="onboarding-header">
                <div className="step-indicator">Passo 2 de 2</div>
                <h1>Adiciona o teu armario</h1>
                <p>Escolhe a categoria, adiciona fotos e a AI classifica cada peca.</p>
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
                <p>Clica para adicionar fotos das tuas roupas</p>
                <p className="drop-sub">JPG, PNG, WEBP ate 10MB. Podes adicionar varias.</p>
            </div>

            {queue.length > 0 && (
                <div className="upload-section">
                    <div className="section-title">
                        <span>Prontos para enviar ({queue.length})</span>
                        {!uploading && (
                            <button className="btn-primary btn-upload-all" onClick={uploadAll}>
                                Classificar com AI
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

            {uploaded.length > 0 && (
                <div className="upload-section">
                    <div className="section-title">
                        <span>Classificados ({uploaded.length})</span>
                    </div>
                    <div className="item-grid">
                        {uploaded.map(item => (
                            <div key={item.id} className="item-card classified">
                                <img src={`http://localhost:8080${item.imageUrl}`} alt={item.subcategory} />
                                <div className="item-meta">
                                    <span className="item-name">{item.color} {item.subcategory}</span>
                                    <span className="tag tag-category">{item.category}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            <div style={{ marginTop: "2rem", display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                {uploaded.length > 0 ? (
                    <button className="btn-primary btn-next" onClick={() => navigate("/dashboard")}>
                        Entrar na app
                    </button>
                ) : (
                    <button className="btn-primary btn-next" disabled>
                        Adiciona pelo menos uma peca para continuar
                    </button>
                )}
            </div>
        </div>
    )
}
