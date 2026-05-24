import { useState, useCallback, useEffect, useRef } from "react"
import { useNavigate } from "react-router-dom"
import { saveStyles } from "../../services/api"
import { useAuth } from "../../context/AuthContext"

// Imagens para Homens
const IMAGES_MEN = [
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
]

// Imagens para Mulheres
// ATENÇÃO: Podes colar os teus próprios links aqui! (Substitui o valor do "url")
const IMAGES_WOMEN = [
    { id: 1, url: "https://i.pinimg.com/1200x/40/2e/73/402e73627cd2e0bec2c5be45d37e753b.jpg", tags: ["streetwear", "oversized", "casual"] },
    { id: 2, url: "https://i.pinimg.com/736x/85/27/21/8527215555c69725e8117d17a9997c72.jpg", tags: ["formal", "tailored", "classic"] },
    { id: 3, url: "https://i.pinimg.com/1200x/65/8e/22/658e22907d88dd6055a98a22b639997a.jpg", tags: ["minimalist", "neutral", "clean"] },
    { id: 4, url: "https://i.pinimg.com/1200x/7b/a2/0b/7ba20bafd80cfb5a935acceb82bd8984.jpg", tags: ["techwear", "utilitarian", "dark"] },
    { id: 5, url: "https://i.pinimg.com/736x/c7/90/0b/c7900be10130e8fcc2e91ea91d5967ce.jpg", tags: ["vintage", "retro", "90s"] },
    { id: 6, url: "https://i.pinimg.com/736x/54/9e/a9/549ea938c484550efbd963b861654252.jpg", tags: ["casual", "comfortable", "everyday"] },
    { id: 7, url: "https://i.pinimg.com/736x/ce/01/e5/ce01e5690f53743945ee4faf486da4f9.jpg", tags: ["streetwear", "sneakers", "urban"] },
    { id: 8, url: "https://i.pinimg.com/736x/85/70/bc/8570bc77931b2bb127d20daf3c34c6c6.jpg", tags: ["formal", "elegant", "office"] },
    { id: 9, url: "https://i.pinimg.com/736x/66/e3/12/66e31263206785529cd940534202b7e3.jpg", tags: ["minimalist", "monochrome", "simple"] },
    { id: 10, url: "https://i.pinimg.com/736x/95/f7/ca/95f7ca555a0ab78c3fe9bbc54f1a4035.jpg", tags: ["athleisure", "sporty", "active"] },
]

export default function SwipeGallery() {
    const { user } = useAuth()
    
    // Escolher a galeria baseada no género, ou misturar as duas se for "Prefiro não dizer"
    const activeImages = user?.gender === "Mulher" ? IMAGES_WOMEN 
                       : user?.gender === "Homem" ? IMAGES_MEN 
                       : [...IMAGES_WOMEN.slice(0, 5), ...IMAGES_MEN.slice(0, 5)]

    const TOTAL = activeImages.length
    const [index, setIndex] = useState(0)
    const [animating, setAnimating] = useState(null)
    const [likedTags, setLikedTags] = useState([])
    const navigate = useNavigate()

    const swipe = useCallback(async (liked) => {
        if (animating) return
        setAnimating(liked ? "right" : "left")

        // Se o utilizador gostar, adicionamos as tags deste estilo à nossa lista
        if (liked) {
            setLikedTags(prev => [...prev, ...activeImages[index].tags])
        }

        setTimeout(async () => {
            setAnimating(null)
            if (index + 1 >= TOTAL) {
                // Chegámos ao fim! Vamos gravar o Style DNA e ir para a Dashboard
                const finalTags = liked ? [...likedTags, ...activeImages[index].tags] : likedTags;
                
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
    }, [index, animating, navigate, likedTags, activeImages])

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
                        <img src={activeImages[index + 1].url} alt="next outfit" />
                    </div>
                )}
                <div className={`swipe-card card-front ${animating === "right" ? "swipe-right" : ""} ${animating === "left" ? "swipe-left" : ""}`}>
                    <img src={activeImages[index].url} alt="outfit" />
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
