const API_URL = "http://localhost:8080"

export async function getTransactions() {
    const res = await fetch(`${API_URL}/transactions`)
    return res.json()
}