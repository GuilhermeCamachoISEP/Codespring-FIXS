import { useState, useCallback, useEffect, useRef } from "react"
import { useNavigate } from "react-router-dom"
import { saveStyles } from "../../services/api"

// Aqui defines os 15 estilos! Podes trocar as URLs para imagens reais do Pinterest/Unsplash.
const IMAGES = [
    { id: 1, url: "https://i.pinimg.com/736x/aa/4a/1a/aa4a1a2fa0cf591b20d0dc25bd03a8ee.jpg", tags: ["streetwear", "oversized", "casual"] },
    { id: 2, url: "https://i.pinimg.com/1200x/44/ba/67/44ba673560a5c147f045a6a56507429c.jpg", tags: ["formal", "tailored", "classic"] },
    { id: 3, url: "https://i.pinimg.com/1200x/68/a0/f3/68a0f398988189366e858f6de5cfa226.jpg", tags: ["minimalist", "neutral", "clean"] },
    { id: 4, url: "https://i.pinimg.com/1200x/0d/de/49/0dde497e85c15bb92b4993b8aadd8104.jpg", tags: ["techwear", "utilitarian", "dark"] },
    { id: 5, url: "https://i.pinimg.com/736x/d2/6f/13/d26f13a8f2ee6166a65c4da02160344d.jpg", tags: ["vintage", "retro", "90s"] },
    { id: 6, url: "https://i.pinimg.com/736x/33/bd/c2/33bdc22842d5a2a56a4c1c1af96e8c0b.jpg", tags: ["casual", "comfortable", "everyday"] },
    { id: 7, url: "https://i.pinimg.com/736x/20/f5/51/20f551bdeb609843d7d78683679f71aa.jpg", tags: ["streetwear", "sneakers", "urban"] },
    { id: 8, url: "https://i.pinimg.com/736x/07/ab/1a/07ab1aea4c2263dbb145be1610fa3d4b.jpg", tags: ["formal", "elegant", "office"] },
    { id: 9, url: "https://i.pinimg.com/736x/f7/ed/0b/f7ed0bf59b10a1c08775a214c63ab293.jpg", tags: ["minimalist", "monochrome", "simple"] },
    { id: 10, url: "https://i.pinimg.com/736x/8c/b7/fc/8cb7fc719f30d827d570b3cf87789ee4.jpg", tags: ["athleisure", "sporty", "active"] },
    { id: 11, url: "https://i.pinimg.com/736x/4b/7c/25/4b7c258ce3d23b9bdae0d927326fee02.jpg", tags: ["y2k", "bold", "trendy"] },
    { id: 12, url: "https://i.pinimg.com/736x/dc/0b/92/dc0b9261464ab4099646f8aad654e8cf.jpg", tags: ["smart-casual", "blazer", "versatile"] },
    { id: 13, url: "https://i.pinimg.com/736x/69/cc/42/69cc424d74937461a0b573097f9bcc2d.jpg", tags: ["alternative", "goth", "edgy"] },
    { id: 14, url: "https://i.pinimg.com/736x/a0/73/c8/a073c853f591aa62b71c39769cd62c05.jpg", tags: ["preppy", "knitwear", "collegiate"] },
    { id: 15, url: "https://i.pinimg.com/1200x/a7/be/5d/a7be5dfad51678251530a3b1c59ba5d7.jpg", tags: ["boho", "earth-tones", "relaxed"] },
]

const TOTAL = IMAGES.length

export default function SwipeGallery() {
    const [index, setIndex] = useState(0)
    const [animating, setAnimating] = useState(null)
    const [likedTags, setLikedTags] = useState([])
    const navigate = useNavigate()

    const swipe = useCallback(async (liked) => {
        if (animating) return
        setAnimating(liked ? "right" : "left")

        // Se o utilizador gostar, adicionamos as tags deste estilo à nossa lista
        if (liked) {
            setLikedTags(prev => [...prev, ...IMAGES[index].tags])
        }

        setTimeout(async () => {
            setAnimating(null)
            if (index + 1 >= TOTAL) {
                // Chegámos ao fim! Vamos gravar o Style DNA e ir para a Dashboard
                const finalTags = liked ? [...likedTags, ...IMAGES[index].tags] : likedTags;
                
                // Contar as tags mais frequentes
                const tagCounts = finalTags.reduce((acc, tag) => {
                    acc[tag] = (acc[tag] || 0) + 1;
                    return acc;
                }, {});
                
                // Pegar nas 5 melhores tags
                const sortedTags = Object.entries(tagCounts)
                    .sort((a, b) => b[1] - a[1])
                    .slice(0, 5);

                const topStylesMap = {};
                sortedTags.forEach(([tag, count]) => {
                    topStylesMap[tag] = count;
                });

                try {
                    // Grava o Style DNA como um mapa
                    await saveStyles(topStylesMap, "Not Specified", "Not Specified", "Not Specified");
                    navigate("/wardrobe") // Em vez de /onboarding/complete, vai logo para a app!
                } catch (err) {
                    console.error("Erro ao gravar estilos:", err)
                    navigate("/wardrobe")
                }
            } else {
                setIndex(i => i + 1)
            }
        }, 300)
    }, [index, animating, navigate, likedTags])

    useEffect(() => {
        function onKey(e) {
            if (e.key === "ArrowRight") swipe(true)
            if (e.key === "ArrowLeft") swipe(false)
        }
        window.addEventListener("keydown", onKey)
        return () => window.removeEventListener("keydown", onKey)
    }, [swipe])

    if (index >= TOTAL) return null

    const progress = ((index) / TOTAL) * 100

    return (
        <div className="swipe-container">
            <div className="onboarding-header">
                <div className="step-indicator">Passo 2 de 2 · {index + 1}/{TOTAL}</div>
                <h1>Gostas deste look?</h1>
                <div className="progress-bar">
                    <div className="progress-fill" style={{ width: `${progress}%` }} />
                </div>
            </div>

            <div className="card-stack">
                {index + 1 < TOTAL && (
                    <div className="swipe-card card-behind">
                        <img src={IMAGES[index + 1].url} alt="next outfit" />
                    </div>
                )}
                <div className={`swipe-card card-front ${animating === "right" ? "swipe-right" : ""} ${animating === "left" ? "swipe-left" : ""}`}>
                    <img src={IMAGES[index].url} alt="outfit" />
                </div>
            </div>

            <div className="swipe-buttons">
                <button
                    className="btn-swipe btn-dislike"
                    onClick={() => swipe(false)}
                    disabled={!!animating}
                >
                    ✕
                    <span>Não</span>
                </button>
                <button
                    className="btn-swipe btn-like"
                    onClick={() => swipe(true)}
                    disabled={!!animating}
                >
                    ♥
                    <span>Gosto</span>
                </button>
            </div>

            <p className="swipe-hint">Podes também usar ← → no teclado</p>
        </div>
    )
}
