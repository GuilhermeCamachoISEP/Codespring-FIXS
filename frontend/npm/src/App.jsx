import { useEffect, useState } from "react"
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom"
import { AuthProvider, useAuth } from "./context/AuthContext"
import { getOnboardingStatus } from "./services/api"
import Login from "./pages/Login"
import Register from "./pages/Register"
import StyleSelect from "./pages/onboarding/StyleSelect"
import SwipeGallery from "./pages/onboarding/SwipeGallery"
import OnboardingWardrobe from "./pages/onboarding/OnboardingWardrobe"
import WardrobeGallery from "./pages/wardrobe/WardrobeGallery"
import WardrobeUpload from "./pages/wardrobe/WardrobeUpload"
import OutfitsPage from "./pages/OutfitsPage"
import ChatPage from "./pages/ChatPage"
import Landing from "./pages/Landing"
import SettingsStyles from "./pages/SettingsStyles"
import EventOutfits from "./pages/EventOutfits"
import ProfilePage from "./pages/ProfilePage"
import Lookbook from "./pages/Lookbook"
import PackingPage from "./pages/PackingPage"

function ProtectedRoute({ children }) {
    const { user } = useAuth()
    const token = localStorage.getItem("token")
    return (user || token) ? children : <Navigate to="/login" replace />
}

function OnboardingGate({ children }) {
    const location = useLocation()
    const [status, setStatus] = useState(null)
    const [error, setError]   = useState("")

    useEffect(() => {
        let active = true
        setError("")
        getOnboardingStatus()
            .then(data => { if (active) setStatus(data) })
            .catch(err  => { if (active) setError(err.message) })
        return () => { active = false }
    }, [location.pathname])

    if (error) {
        // Only send to login for real auth failures (expired / invalid token)
        const isAuthError = error.includes("401")
            || error.toLowerCase().includes("unauthorized")
            || error.toLowerCase().includes("token")
        if (isAuthError) return <Navigate to="/login" replace />

        // If we already have a status from a previous fetch, keep rendering
        if (status) {
            const isOnboarding = location.pathname.startsWith("/onboarding")
            if (!status.complete && !isOnboarding) return <Navigate to={status.nextStep || "/onboarding/styles"} replace />
            if (status.complete && isOnboarding) return <Navigate to="/outfits" replace />
            return children
        }

        // No status yet and backend unreachable — show retry instead of looping
        return (
            <div className="loading-state" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "16px" }}>
                <div style={{ fontSize: "2rem" }}>⚠️</div>
                <p>Não foi possível ligar ao servidor.</p>
                <p style={{ fontSize: "0.85rem", color: "var(--color-text-muted)" }}>
                    Confirma que o backend está a correr em localhost:8080
                </p>
                <button className="btn btn-primary" onClick={() => window.location.reload()}>
                    Tentar novamente
                </button>
            </div>
        )
    }

    if (!status) {
        return (
            <div className="loading-state" style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
                A preparar a tua app...
            </div>
        )
    }

    const isOnboarding = location.pathname.startsWith("/onboarding")
    if (!status.complete && !isOnboarding) return <Navigate to={status.nextStep || "/onboarding/styles"} replace />
    if (status.complete && isOnboarding) return <Navigate to="/outfits" replace />

    return children
}

function AppRoutes() {
    const { user } = useAuth()
    return (
        <Routes>
            <Route path="/login" element={user ? <Navigate to="/outfits" replace /> : <Login />} />
            <Route path="/register" element={user ? <Navigate to="/outfits" replace /> : <Register />} />
            <Route path="/onboarding/styles" element={<ProtectedRoute><SwipeGallery /></ProtectedRoute>} />
            <Route path="/onboarding/wardrobe" element={<ProtectedRoute><OnboardingGate><OnboardingWardrobe /></OnboardingGate></ProtectedRoute>} />
            <Route path="/outfits" element={<ProtectedRoute><OnboardingGate><OutfitsPage /></OnboardingGate></ProtectedRoute>} />
            <Route path="/wardrobe" element={<ProtectedRoute><OnboardingGate><WardrobeGallery /></OnboardingGate></ProtectedRoute>} />
            <Route path="/wardrobe/upload" element={<ProtectedRoute><OnboardingGate><WardrobeUpload /></OnboardingGate></ProtectedRoute>} />
            <Route path="/chat" element={<ProtectedRoute><OnboardingGate><ChatPage /></OnboardingGate></ProtectedRoute>} />
            <Route path="/preferences" element={<ProtectedRoute><OnboardingGate><SettingsStyles /></OnboardingGate></ProtectedRoute>} />
            <Route path="/events/outfits" element={<ProtectedRoute><OnboardingGate><EventOutfits /></OnboardingGate></ProtectedRoute>} />
            <Route path="/settings/styles" element={<Navigate to="/preferences" replace />} />
            <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
            <Route path="/lookbook" element={<ProtectedRoute><OnboardingGate><Lookbook /></OnboardingGate></ProtectedRoute>} />
            <Route path="/packing" element={<ProtectedRoute><OnboardingGate><PackingPage /></OnboardingGate></ProtectedRoute>} />
            <Route path="/dashboard" element={<Navigate to={user ? "/outfits" : "/login"} replace />} />
            <Route path="/" element={<Navigate to={user ? "/outfits" : "/login"} replace />} />
            <Route path="*" element={<Navigate to={user ? "/outfits" : "/login"} replace />} />
        </Routes>
    )
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AppRoutes />
            </AuthProvider>
        </BrowserRouter>
    )
}
