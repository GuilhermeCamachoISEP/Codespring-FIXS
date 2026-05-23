import { useEffect, useState } from "react"
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom"
import { AuthProvider, useAuth } from "./context/AuthContext"
import { getOnboardingStatus } from "./services/api"
import Login from "./pages/Login"
import Register from "./pages/Register"
import StyleSelect from "./pages/onboarding/StyleSelect"
import OnboardingWardrobe from "./pages/onboarding/OnboardingWardrobe"
import Dashboard from "./pages/Dashboard"
import WardrobeGallery from "./pages/wardrobe/WardrobeGallery"
import WardrobeUpload from "./pages/wardrobe/WardrobeUpload"
import OutfitsPage from "./pages/OutfitsPage"

function ProtectedRoute({ children }) {
    const { user } = useAuth()
    const token = localStorage.getItem("token")
    return (user || token) ? children : <Navigate to="/login" replace />
}

function OnboardingGate({ children }) {
    const location = useLocation()
    const [status, setStatus] = useState(null)
    const [error, setError] = useState("")

    useEffect(() => {
        let active = true
        getOnboardingStatus()
            .then(data => { if (active) setStatus(data) })
            .catch(err => { if (active) setError(err.message) })
        return () => { active = false }
    }, [location.pathname])

    if (error) {
        return <Navigate to="/login" replace />
    }

    if (!status) {
        return <div className="loading-state">A preparar a tua app...</div>
    }

    const isOnboarding = location.pathname.startsWith("/onboarding")
    if (!status.complete && !isOnboarding) {
        return <Navigate to={status.nextStep || "/onboarding/styles"} replace />
    }

    if (status.complete && location.pathname.startsWith("/onboarding")) {
        return <Navigate to="/dashboard" replace />
    }

    return children
}

function AppRoutes() {
    const { user } = useAuth()
    return (
        <Routes>
            <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <Login />} />
            <Route path="/register" element={user ? <Navigate to="/dashboard" replace /> : <Register />} />
            <Route path="/onboarding/styles" element={<ProtectedRoute><OnboardingGate><StyleSelect /></OnboardingGate></ProtectedRoute>} />
            <Route path="/onboarding/wardrobe" element={<ProtectedRoute><OnboardingGate><OnboardingWardrobe /></OnboardingGate></ProtectedRoute>} />
            <Route path="/dashboard" element={<ProtectedRoute><OnboardingGate><Dashboard /></OnboardingGate></ProtectedRoute>} />
            <Route path="/wardrobe" element={<ProtectedRoute><OnboardingGate><WardrobeGallery /></OnboardingGate></ProtectedRoute>} />
            <Route path="/wardrobe/upload" element={<ProtectedRoute><OnboardingGate><WardrobeUpload /></OnboardingGate></ProtectedRoute>} />
            <Route path="/outfits" element={<ProtectedRoute><OnboardingGate><OutfitsPage /></OnboardingGate></ProtectedRoute>} />
            <Route path="*" element={<Navigate to={user ? "/dashboard" : "/login"} replace />} />
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
