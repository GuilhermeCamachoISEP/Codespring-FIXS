import { useState, useCallback, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { saveSwipe } from "../../services/api"

const TOTAL = 50

const IMAGES = Array.from({ length: TOTAL }, (_, i) => ({
    id: `outfit_${i + 1}`,
    url: `https://picsum.photos/seed/${i + 10}/320/480`,
}))

export default function SwipeGallery() {
    const [index, setIndex] = useState(0)
    const [animating, setAnimating] = useState(null)
    const navigate = useNavigate()

    const swipe = useCallback(async (liked) => {
        if (animating) return
        setAnimating(liked ? "right" : "left")

        try {
            await saveSwipe(IMAGES[index].id, liked)
        } catch (err) {
            console.error(err)
        }

        setTimeout(() => {
            setAnimating(null)
            if (index + 1 >= TOTAL) {
                navigate("/onboarding/complete")
            } else {
                setIndex(i => i + 1)
            }
        }, 300)
    }, [index, animating, navigate])

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
