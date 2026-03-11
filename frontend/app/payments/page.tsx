"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ChevronDown, ChevronRight, AlertCircle, Check, Plus, Banknote } from "lucide-react"
import { fetchClients, fetchGroupedPayments, markPaymentAsPaid, type Client, type GroupedPaymentResponse } from "@/lib/api"
import { cn } from "@/lib/utils"
import { PaymentDetailsDialog } from "@/components/payment-details-dialog"
import { formatDisplayCurrency } from "@/lib/format"

export default function PaymentsPage() {
  const router = useRouter()
  const [clients, setClients] = useState<Client[]>([])
  const [payments, setPayments] = useState<GroupedPaymentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedRows, setExpandedRows] = useState<Record<number, boolean>>({})

  // Filters
  const [selectedClient, setSelectedClient] = useState<string>("all")
  const [selectedStatus, setSelectedStatus] = useState<string>("all")
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1)
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear())

  useEffect(() => {
    fetchClients().then(setClients).catch(console.error)
  }, [])

  useEffect(() => {
    setLoading(true)
    fetchGroupedPayments({
      clientId: selectedClient,
      status: selectedStatus,
      month: selectedMonth,
      year: selectedYear,
    })
      .then(setPayments)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [selectedClient, selectedStatus, selectedMonth, selectedYear])

  const toggleRow = (id: number) => {
    setExpandedRows((prev) => ({ ...prev, [id]: !prev[id] }))
  }

  const refreshPayments = async () => {
    try {
      const updatedPayments = await fetchGroupedPayments({
        clientId: selectedClient,
        status: selectedStatus,
        month: selectedMonth,
        year: selectedYear,
      })
      setPayments(updatedPayments)
    } catch (error) {
      console.error("Failed to refresh payments:", error)
    }
  }

  const handleMarkAsPaid = async (paymentId: number) => {
    try {
      await markPaymentAsPaid(paymentId)
      // Refresh payments after marking as paid
      await refreshPayments()
    } catch (error) {
      console.error("Failed to mark payment as paid:", error)
    }
  }

  const formatDate = (dateString: string) => {
    // Parse date as local time to avoid timezone issues
    const [year, month, day] = dateString.split('-')
    const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day))
    return date.toLocaleDateString("pt-BR")
  }

  const getStatusBadge = (status: string) => {
    const baseBadgeClass = "whitespace-nowrap px-2 py-0.5 text-xs font-semibold"
    switch (status) {
      case "PAID":
        return <Badge className={`${baseBadgeClass} bg-green-500 hover:bg-green-600`}>Pago</Badge>
      case "PAID_LATE":
        return <Badge className={`${baseBadgeClass} bg-yellow-600 hover:bg-yellow-700`}>Pago com Atraso</Badge>
      case "OVERDUE":
        return <Badge variant="destructive" className={baseBadgeClass}>Atrasado</Badge>
      default:
        return <Badge variant="secondary" className={baseBadgeClass}>Pendente</Badge>
    }
  }

  const months = [
    { value: 1, label: "Janeiro" },
    { value: 2, label: "Fevereiro" },
    { value: 3, label: "Março" },
    { value: 4, label: "Abril" },
    { value: 5, label: "Maio" },
    { value: 6, label: "Junho" },
    { value: 7, label: "Julho" },
    { value: 8, label: "Agosto" },
    { value: 9, label: "Setembro" },
    { value: 10, label: "Outubro" },
    { value: 11, label: "Novembro" },
    { value: 12, label: "Dezembro" },
  ]

  const years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - 2 + i)

  return (
    <main className="sm:ml-14 p-4">
      <div className="flex flex-col gap-6">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-gray-800">Pagamentos</h1>
          <Button
            onClick={() => router.push("/payment-groups/new")}
            size="sm"
            className="gap-2"
          >
            <Plus className="h-4 w-4" />
            Novo Grupo
          </Button>
        </div>

        <Card>
          <CardHeader className="pb-3">
            <div className="flex flex-wrap gap-4 items-end">
              <div className="w-48">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Cliente</label>
                <Select value={selectedClient} onValueChange={setSelectedClient}>
                  <SelectTrigger>
                    <SelectValue placeholder="Todos os Clientes" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">Todos os Clientes</SelectItem>
                    {clients.map((client) => (
                      <SelectItem key={client.id} value={client.id.toString()}>
                        {client.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="w-40">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Situação</label>
                <Select value={selectedStatus} onValueChange={setSelectedStatus}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">Todas</SelectItem>
                    <SelectItem value="PENDING">Pendente</SelectItem>
                    <SelectItem value="PAID">Pago</SelectItem>
                    <SelectItem value="OVERDUE">Atrasado</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="w-40">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Mês</label>
                <Select value={selectedMonth.toString()} onValueChange={(value) => setSelectedMonth(parseInt(value))}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {months.map((m) => (
                      <SelectItem key={m.value} value={m.value.toString()}>
                        {m.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="w-32">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Ano</label>
                <Select value={selectedYear.toString()} onValueChange={(value) => setSelectedYear(parseInt(value))}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {years.map((y) => (
                      <SelectItem key={y} value={y.toString()}>
                        {y}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border overflow-x-auto">
              <Table className="table-fixed w-full min-w-[1200px]">
                <colgroup>
                  <col className="w-[4%]" />
                  <col className="w-[20%]" />
                  <col className="w-[8%]" />
                  <col className="w-[12%]" />
                  <col className="w-[12%]" />
                  <col className="w-[12%]" />
                  <col className="w-[12%]" />
                  <col className="w-[10%]" />
                  <col className="w-[10%]" />
                </colgroup>
                <TableHeader>
                  <TableRow>
                    <TableHead></TableHead>
                    <TableHead>Pagador</TableHead>
                    <TableHead className="text-center">Parcela</TableHead>
                    <TableHead>Vencimento</TableHead>
                    <TableHead>Data Pagamento</TableHead>
                    <TableHead className="text-right">Valor Original</TableHead>
                    <TableHead className="text-right">Valor Com Juros</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead className="text-right"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={9} className="h-24 text-center">
                        Carregando...
                      </TableCell>
                    </TableRow>
                  ) : payments.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={9} className="h-40">
                        <div className="flex flex-col items-center justify-center gap-4 text-muted-foreground">
                          <Banknote className="h-12 w-12 opacity-50" />
                          <div className="text-center">
                            <p className="font-medium">Nenhum pagamento encontrado</p>
                            <p className="text-sm">Crie um novo grupo de pagamentos para começar</p>
                          </div>
                          <Button className="gap-2" onClick={() => router.push("/payment-groups/new")}>
                            <Plus className="h-4 w-4" />
                            Novo Grupo
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ) : (
                    payments.map((group) => (
                      <>
                        <TableRow
                          key={group.mainPayment.id}
                          className={cn(group.overduePayments.length > 0 && "bg-orange-50/30")}
                        >
                          <TableCell>
                            {group.overduePayments.length > 0 && (
                              <Button
                                variant="ghost"
                                size="icon-sm"
                                onClick={() => toggleRow(group.mainPayment.id)}
                              >
                                {expandedRows[group.mainPayment.id] ? (
                                  <ChevronDown className="h-4 w-4" />
                                ) : (
                                  <ChevronRight className="h-4 w-4" />
                                )}
                              </Button>
                            )}
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-col">
                              <span className="font-medium">{group.mainPayment.payerName}</span>
                            </div>
                          </TableCell>
                          <TableCell className="text-center">
                            {group.mainPayment.installmentNumber} / {group.mainPayment.totalInstallments}
                          </TableCell>
                          <TableCell>{formatDate(group.mainPayment.dueDate)}</TableCell>
                          <TableCell>
                            {group.mainPayment.paymentDate ? formatDate(group.mainPayment.paymentDate) : "---"}
                          </TableCell>
                          <TableCell className="text-right">{formatDisplayCurrency(group.mainPayment.originalValue)}</TableCell>
                          <TableCell className="text-right">
                            <div className="flex flex-col items-end">
                              <span>
                                {group.mainPayment.paymentStatus === "PAID_LATE" && group.mainPayment.overdueValue
                                  ? formatDisplayCurrency(group.mainPayment.overdueValue)
                                  : group.mainPayment.overdueValue
                                  ? formatDisplayCurrency(group.mainPayment.overdueValue)
                                  : "---"}
                              </span>
                              {group.overduePayments.length > 0 && (
                                <span className="text-[10px] text-orange-600 flex items-center gap-0.5 font-bold">
                                  <AlertCircle className="h-3 w-3" />
                                  Possui parcelas atrasadas
                                </span>
                              )}
                            </div>
                          </TableCell>
                          <TableCell className="text-center">{getStatusBadge(group.mainPayment.paymentStatus)}</TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2">
                              <PaymentDetailsDialog 
                                payment={group.mainPayment} 
                                onSuccess={refreshPayments}
                              />
                              {group.mainPayment.paymentStatus !== "PAID" && group.mainPayment.paymentStatus !== "PAID_LATE" && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="h-8 gap-1"
                                  onClick={() => handleMarkAsPaid(group.mainPayment.id)}
                                >
                                  <Check className="h-3 w-3" />
                                  Pagar
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>

                        {expandedRows[group.mainPayment.id] && group.overduePayments.map((overdue) => (
                          <TableRow key={overdue.id} className="bg-muted/30">
                            <TableCell></TableCell>
                            <TableCell className="pl-8 text-xs text-muted-foreground italic">
                              Parcela atrasada do grupo
                            </TableCell>
                            <TableCell className="text-xs text-center">
                              {overdue.installmentNumber} / {overdue.totalInstallments}
                            </TableCell>
                            <TableCell className="text-xs">{formatDate(overdue.dueDate)}</TableCell>
                            <TableCell className="text-xs">
                              {overdue.paymentDate ? formatDate(overdue.paymentDate) : "---"}
                            </TableCell>
                            <TableCell className="text-xs text-right">{formatDisplayCurrency(overdue.originalValue)}</TableCell>
                            <TableCell className="text-xs text-right">
                              <span className={overdue.paymentStatus === "PAID_LATE" ? "font-medium text-yellow-700" : overdue.paymentStatus === "OVERDUE" ? "font-medium text-destructive" : ""}>
                                {overdue.paymentStatus === "PAID_LATE" && overdue.overdueValue
                                  ? formatDisplayCurrency(overdue.overdueValue)
                                  : overdue.overdueValue
                                  ? formatDisplayCurrency(overdue.overdueValue)
                                  : "---"}
                              </span>
                            </TableCell>
                            <TableCell className="text-center">{getStatusBadge(overdue.paymentStatus)}</TableCell>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <PaymentDetailsDialog 
                                  payment={overdue} 
                                  onSuccess={refreshPayments}
                                />
                                {overdue.paymentStatus !== "PAID" && overdue.paymentStatus !== "PAID_LATE" && (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-7 gap-1 text-xs"
                                    onClick={() => handleMarkAsPaid(overdue.id)}
                                  >
                                    <Check className="h-3 w-3" />
                                    Pagar
                                  </Button>
                                )}
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                      </>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}
