"use client"

import { useState, useEffect, useMemo } from "react"
import { useRouter } from "next/navigation"
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  getExpandedRowModel,
  flexRender,
  type SortingState,
  type ExpandedState,
} from "@tanstack/react-table"
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
import { Button } from "@/components/ui/button"
import { Plus, Banknote } from "lucide-react"
import { fetchClients, fetchGroupedPayments, markPaymentAsPaid, type Client, type GroupedPaymentResponse } from "@/lib/api"
import { cn } from "@/lib/utils"
import { getPaymentColumns, type PaymentRow } from "./columns"

export default function PaymentsPage() {
  const router = useRouter()
  const [clients, setClients] = useState<Client[]>([])
  const [payments, setPayments] = useState<GroupedPaymentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [sorting, setSorting] = useState<SortingState>([])
  const [expanded, setExpanded] = useState<ExpandedState>({})

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

  const data: PaymentRow[] = useMemo(
    () => payments.map((group) => ({ ...group.mainPayment, subRows: group.overduePayments })),
    [payments]
  )

  const columns = getPaymentColumns({ onMarkAsPaid: handleMarkAsPaid, onRefresh: refreshPayments })

  const table = useReactTable({
    data,
    columns,
    state: { sorting, expanded },
    onSortingChange: setSorting,
    onExpandedChange: setExpanded,
    getSubRows: (row) => row.subRows,
    getRowId: (row) => row.id.toString(),
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getExpandedRowModel: getExpandedRowModel(),
  })

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
                  {table.getHeaderGroups().map((headerGroup) => (
                    <TableRow key={headerGroup.id}>
                      {headerGroup.headers.map((header) => (
                        <TableHead key={header.id}>
                          {header.isPlaceholder
                            ? null
                            : flexRender(header.column.columnDef.header, header.getContext())}
                        </TableHead>
                      ))}
                    </TableRow>
                  ))}
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={9} className="h-24 text-center">
                        Carregando...
                      </TableCell>
                    </TableRow>
                  ) : data.length === 0 ? (
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
                    table.getRowModel().rows.map((row) => (
                      <TableRow
                        key={row.id}
                        className={cn(
                          row.depth === 0 && (row.original.subRows?.length ?? 0) > 0 && "bg-orange-50/30",
                          row.depth > 0 && "bg-muted/30"
                        )}
                      >
                        {row.getVisibleCells().map((cell) => (
                          <TableCell key={cell.id}>
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </TableCell>
                        ))}
                      </TableRow>
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
