import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import './index.css'
import App from './App.jsx'

// Initialize dark mode (default: dark)
const savedDark = localStorage.getItem('darkMode')
const isDark = savedDark !== null ? JSON.parse(savedDark) : true
document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
if (savedDark === null) localStorage.setItem('darkMode', 'true')

// Substitui pela tua chave gerada na Google Cloud Console
const GOOGLE_CLIENT_ID = "647442385117-jl1l4q65nt8tn7jmh6n0h9p659ul7evd.apps.googleusercontent.comw"

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </StrictMode>,
)
