import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

// Initialize dark mode (default: dark)
const savedDark = localStorage.getItem('darkMode')
const isDark = savedDark !== null ? JSON.parse(savedDark) : true
document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
if (savedDark === null) localStorage.setItem('darkMode', 'true')

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
