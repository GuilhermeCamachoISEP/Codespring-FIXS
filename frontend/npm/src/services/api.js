const API_URL = "http://localhost:8080"

function getToken() {
    return localStorage.getItem("token")
}

function authHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${getToken()}`
    }
}

export async function register(email, password, name) {
    const res = await fetch(`${API_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, name })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function login(email, password) {
    const res = await fetch(`${API_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
    })
    if (!res.ok) throw new Error("Invalid credentials")
    return res.json()
}

export async function saveStyles(styles, gender, ageRange, budgetRange) {
    const res = await fetch(`${API_URL}/onboarding/styles`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ styles, gender, ageRange, budgetRange })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function saveSwipe(imageId, liked) {
    const res = await fetch(`${API_URL}/onboarding/swipe`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ imageId, liked })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function uploadClothingItem(file) {
    const form = new FormData()
    form.append("file", file)
    const res = await fetch(`${API_URL}/wardrobe/upload`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${getToken()}` },
        body: form
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getWardrobe(category) {
    const url = category
        ? `${API_URL}/wardrobe?category=${category}`
        : `${API_URL}/wardrobe`
    const res = await fetch(url, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function deleteWardrobeItem(id) {
    const res = await fetch(`${API_URL}/wardrobe/${id}`, {
        method: "DELETE",
        headers: authHeaders()
    })
    if (!res.ok) throw new Error(await res.text())
}

export async function getWardrobeCount() {
    const res = await fetch(`${API_URL}/wardrobe/count`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getTransactions() {
    const res = await fetch(`${API_URL}/transactions`)
    return res.json()
}
