"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Clock, CheckCircle, AlertCircle, Users } from "lucide-react"
import { fetchGroupedPayments, fetchClients, type GroupedPaymentResponse } from "@/lib/api"
import { formatDisplayCurrency } from "@/lib/format"

interface Stats {
  pending: { count: number; total: number }
  paid: { count: number; total: number }
  overdue: { count: number; total: number }
  totalClients: number
}

function computeStats(groups: GroupedPaymentResponse[]): Omit<Stats, "totalClients"> {
  const stats = {
    pending: { count: 0, total: 0 },
    paid: { count: 0, total: 0 },
    overdue: { count: 0, total: 0 },
  }

  const processPayment = (p: GroupedPaymentResponse["mainPayment"]) => {
    switch (p.paymentStatus) {
      case "PENDING":
        stats.pending.count++
        stats.pending.total += p.originalValue
        break
      case "PAID":
      case "PAID_LATE":
        stats.paid.count++
        stats.paid.total += p.originalValue
        break
      case "OVERDUE":
        stats.overdue.count++
        stats.overdue.total += p.originalValue
        break
    }
  }

  for (const group of groups) {
    processPayment(group.mainPayment)
    for (const overdue of group.overduePayments) {
      processPayment(overdue)
    }
  }

  return stats
}

export default function Home() {
  const [stats, setStats] = useState<Stats>({
    pending: { count: 0, total: 0 },
    paid: { count: 0, total: 0 },
    overdue: { count: 0, total: 0 },
    totalClients: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const now = new Date()
    const month = now.getMonth() + 1
    const year = now.getFullYear()

    Promise.all([
      fetchGroupedPayments({ month, year }),
      fetchClients(),
    ])
      .then(([groups, clients]) => {
        const computed = computeStats(groups)
        setStats({ ...computed, totalClients: clients.length })
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const cards = [
    {
      title: "Pendentes",
      count: stats.pending.count,
      total: stats.pending.total,
      icon: Clock,
      color: "text-amber-600",
      bg: "bg-amber-50",
    },
    {
      title: "Pagos",
      count: stats.paid.count,
      total: stats.paid.total,
      icon: CheckCircle,
      color: "text-green-600",
      bg: "bg-green-50",
    },
    {
      title: "Atrasados",
      count: stats.overdue.count,
      total: stats.overdue.total,
      icon: AlertCircle,
      color: "text-red-600",
      bg: "bg-red-50",
    },
    {
      title: "Clientes",
      count: stats.totalClients,
      total: null,
      icon: Users,
      color: "text-blue-600",
      bg: "bg-blue-50",
    },
  ]

  return (
    <main className="sm:ml-14 p-4">
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold text-gray-800">Dashboard</h1>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {cards.map((card) => (
            <Card key={card.title}>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {card.title}
                </CardTitle>
                <div className={`${card.bg} ${card.color} p-2 rounded-lg`}>
                  <card.icon className="h-4 w-4" />
                </div>
              </CardHeader>
              <CardContent>
                {loading ? (
                  <p className="text-sm text-muted-foreground">Carregando...</p>
                ) : (
                  <>
                    <p className="text-2xl font-bold">{card.count}</p>
                    {card.total !== null && (
                      <p className="text-xs text-muted-foreground">
                        {formatDisplayCurrency(card.total)}
                      </p>
                    )}
                  </>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </main>
  )
}
