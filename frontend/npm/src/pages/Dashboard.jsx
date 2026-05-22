import { useEffect, useState } from "react"
import { getTransactions } from "../services/api"

function Dashboard() {
    const [data, setData] = useState([])

    useEffect(() => {
        getTransactions().then(setData)
    }, [])

    return (
        <div>
            <h2>Dashboard</h2>

            {data.length === 0 && <p>Sem dados ainda</p>}

            {data.map((item) => (
                <div key={item.id}>
                    {item.name}
                </div>
            ))}
        </div>
    )
}

export default Dashboard