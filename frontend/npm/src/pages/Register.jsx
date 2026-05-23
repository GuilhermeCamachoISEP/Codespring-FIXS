import { useState } from "react"
import { useNavigate, Link } from "react-router-dom"
import { register } from "../services/api"
import { useAuth } from "../context/AuthContext"

export default function Register() {
    const [name, setName] = useState("")
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [error, setError] = useState("")
    const [loading, setLoading] = useState(false)
    const { saveAuth } = useAuth()
    const navigate = useNavigate()

    async function handleSubmit(e) {
        e.preventDefault()
        setError("")
        setLoading(true)
        try {
            const data = await register(email, password, name)
            saveAuth(data)
            navigate("/onboarding/styles")
        } catch (err) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-logo">STYLIST AI</div>
                <h2>Criar conta</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Nome</label>
                        <input
                            type="text"
                            value={name}
                            onChange={e => setName(e.target.value)}
                            placeholder="O teu nome"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            placeholder="email@exemplo.com"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                            minLength={6}
                        />
                    </div>
                    {error && <p className="error">{error}</p>}
                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? "A criar..." : "Criar conta"}
                    </button>
                </form>
                <p className="auth-link">
                    Já tens conta? <Link to="/login">Entrar</Link>
                </p>
            </div>
        </div>
    )
}
