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

export async function register(email, password, name, gender) {
    const res = await fetch(`${API_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, name, gender })
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

export async function getStyles() {
    const res = await fetch(`${API_URL}/onboarding/styles`, {
        headers: authHeaders()
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getOnboardingStatus() {
    const res = await fetch(`${API_URL}/onboarding/status`, {
        headers: authHeaders()
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getAiStatus() {
    const res = await fetch(`${API_URL}/ai/status`)
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

export async function uploadClothingItem(file, categoryHint) {
    const form = new FormData()
    form.append("file", file)
    if (categoryHint) form.append("categoryHint", categoryHint)
    const res = await fetch(`${API_URL}/wardrobe/upload`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${getToken()}` },
        body: form
    })
    if (!res.ok) {
        const message = await res.text()
        throw new Error(message || "AI classification failed")
    }
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

export async function deleteWardrobeItem(id, force = false) {
    const res = await fetch(`${API_URL}/wardrobe/${id}?force=${force}`, {
        method: "DELETE",
        headers: authHeaders()
    })
    if (!res.ok) {
        if (res.status === 409) throw new Error("RESERVED")
        throw new Error(await res.text())
    }
}

export async function getWardrobeCount() {
    const res = await fetch(`${API_URL}/wardrobe/count`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getOutfits(lat, lon) {
    const params = lat != null && lon != null ? `?lat=${lat}&lon=${lon}` : ""
    const res = await fetch(`${API_URL}/outfits${params}`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json() // returns { outfits, weather, advisory }
}

export async function saveOutfitHistory(outfitItems) {
    const res = await fetch(`${API_URL}/outfits/history`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ outfitItems })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getOutfitHistory() {
    const res = await fetch(`${API_URL}/outfits/history`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getLikedOutfits() {
    const res = await fetch(`${API_URL}/outfits/history/liked`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getReservedOutfits() {
    const res = await fetch(`${API_URL}/outfits/history/reserved`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function saveOutfitHistoryForEvent(outfitItems, eventName) {
    const res = await fetch(`${API_URL}/outfits/history`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ outfitItems, eventName })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function toggleOutfitLike(id) {
    const res = await fetch(`${API_URL}/outfits/history/${id}/like`, {
        method: "PATCH",
        headers: authHeaders()
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function markOutfitWorn(id) {
    const res = await fetch(`${API_URL}/outfits/history/${id}/worn`, {
        method: "PATCH",
        headers: authHeaders()
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getWeather(lat, lon) {
    const res = await fetch(`${API_URL}/weather?lat=${lat}&lon=${lon}`, { headers: authHeaders() })
    if (!res.ok) {
        const message = await res.text()
        throw new Error(message || `Weather request failed (${res.status})`)
    }
    const data = await res.json()
    if (!data || data.temperature == null) {
        throw new Error("Weather data unavailable")
    }
    return data
}

export async function getTransactions() {
    const res = await fetch(`${API_URL}/transactions`)
    return res.json()
}

export async function sendChatMessage(message, history, lat, lon, mode = "conversational") {
    const res = await fetch(`${API_URL}/chat`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ message, history, lat, lon, mode })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function refineOutfit(message, history, context, lat, lon) {
    const body = { message, mode: "outfit-refine", history, context }
    if (lat != null && lon != null) { body.lat = lat; body.lon = lon; }
    const res = await fetch(`${API_URL}/chat`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify(body)
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function planPacking(tripDescription, travelDate) {
    const res = await fetch(`${API_URL}/packing`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ tripDescription, travelDate })
    })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getOutfitsForEvent(eventName, date, lat, lon) {
    let params = `?eventName=${encodeURIComponent(eventName)}&date=${date}`
    if (lat != null && lon != null) params += `&lat=${lat}&lon=${lon}`
    const res = await fetch(`${API_URL}/outfits/event${params}`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function reserveOutfit(eventName, eventDate, itemIds) {
    const res = await fetch(`${API_URL}/outfits/reserve`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ eventName, eventDate, itemIds })
    })
    if (!res.ok) throw new Error(await res.text())
}

// ─── Inspiration (Unsplash) ───────────────────────────────────────────────────

export async function getInspiration() {
    const res = await fetch(`${API_URL}/inspiration/photos`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json() // { sections: [...], configured: bool }
}

// ─── Pinterest ────────────────────────────────────────────────────────────────

export async function getPinterestStatus() {
    const res = await fetch(`${API_URL}/pinterest/status`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json() // { connected: bool, configured: bool }
}

export async function getPinterestAuthUrl() {
    const res = await fetch(`${API_URL}/pinterest/auth-url`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json() // { url: string }
}

export async function getPinterestBoards() {
    const res = await fetch(`${API_URL}/pinterest/boards`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function getPinterestBoardPins(boardId) {
    const res = await fetch(`${API_URL}/pinterest/boards/${boardId}/pins`, { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    return res.json()
}

export async function disconnectPinterest() {
    const res = await fetch(`${API_URL}/pinterest/disconnect`, {
        method: "DELETE",
        headers: authHeaders()
    })
    if (!res.ok) throw new Error(await res.text())
}
