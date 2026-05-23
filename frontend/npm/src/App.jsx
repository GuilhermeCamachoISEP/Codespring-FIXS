import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { AuthProvider, useAuth } from "./context/AuthContext"
import Login from "./pages/Login"
import Register from "./pages/Register"
import StyleSelect from "./pages/onboarding/StyleSelect"
import SwipeGallery from "./pages/onboarding/SwipeGallery"
import OnboardingComplete from "./pages/onboarding/OnboardingComplete"
import Dashboard from "./pages/Dashboard"
import WardrobeGallery from "./pages/wardrobe/WardrobeGallery"
import WardrobeUpload from "./pages/wardrobe/WardrobeUpload"

function ProtectedRoute({ children }) {
    const { user } = useAuth()
    return user ? children : <Navigate to="/login" replace />
}

function AppRoutes() {
    const { user } = useAuth()
    return (
        <Routes>
            <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <Login />} />
            <Route path="/register" element={user ? <Navigate to="/dashboard" replace /> : <Register />} />
            <Route path="/onboarding/styles" element={<ProtectedRoute><StyleSelect /></ProtectedRoute>} />
            <Route path="/onboarding/swipe" element={<ProtectedRoute><SwipeGallery /></ProtectedRoute>} />
            <Route path="/onboarding/complete" element={<ProtectedRoute><OnboardingComplete /></ProtectedRoute>} />
            <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
            <Route path="/wardrobe" element={<ProtectedRoute><WardrobeGallery /></ProtectedRoute>} />
            <Route path="/wardrobe/upload" element={<ProtectedRoute><WardrobeUpload /></ProtectedRoute>} />
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
