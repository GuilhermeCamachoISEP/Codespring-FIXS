import { Link } from "react-router-dom"

export default function Landing() {
    return (
        <div className="auth-container">
            <div className="auth-card" style={{ textAlign: "center", padding: "3rem 2rem" }}>
                <div className="auth-logo" style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>Gaveta.</div>
                <h2 style={{ marginBottom: "2rem" }}>Bem-vindo</h2>
                <p style={{ color: "#aaa", marginBottom: "2rem" }}>
                    A tua AI pessoal que escolhe outfits para o teu dia consoante os estilos de roupa que gostas, a meteorologia e as roupas do teu armário.
                </p>
                
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                    <Link to="/login" className="btn-primary" style={{ textDecoration: "none", textAlign: "center" }}>
                        Entrar
                    </Link>
                    <Link to="/register" className="btn-outline" style={{ textDecoration: "none", textAlign: "center", display: "block" }}>
                        Criar conta
                    </Link>
                </div>
            </div>
        </div>
    )
}
