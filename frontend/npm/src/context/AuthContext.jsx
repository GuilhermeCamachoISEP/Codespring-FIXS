import { createContext, useContext, useState } from "react"

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = localStorage.getItem("user")
        return stored ? JSON.parse(stored) : null
    })

    function saveAuth(authData) {
        localStorage.setItem("token", authData.token)
        localStorage.setItem("user", JSON.stringify({
            id: authData.userId,
            email: authData.email,
            name: authData.name
        }))
        setUser({ id: authData.userId, email: authData.email, name: authData.name })
    }

    function logout() {
        localStorage.removeItem("token")
        localStorage.removeItem("user")
        localStorage.removeItem("stylist_outfit_cache")
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, saveAuth, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}
