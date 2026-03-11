"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Card, CardContent } from "@/components/ui/card"
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Plus, FolderOpen, Trash2 } from "lucide-react"
import { fetchClients, fetchPaymentGroups, deletePaymentGroup, type Client, type PaymentGroupListItem } from "@/lib/api"
import { formatDisplayCurrency, formatDisplayDocument } from "@/lib/format"

export default function PaymentGroupsPage() {
  const router = useRouter()
  const [clients, setClients] = useState<Client[]>([])
  const [groups, setGroups] = useState<PaymentGroupListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedClient, setSelectedClient] = useState<string>("all")

  useEffect(() => {
    fetchClients().then(setClients).catch(console.error)
  }, [])

  useEffect(() => {
    loadGroups()
  }, [selectedClient])

  const loadGroups = async () => {
    setLoading(true)
    try {
      const data = await fetchPaymentGroups({ clientId: selectedClient })
      setGroups(data)
    } catch (error) {
      console.error("Failed to load payment groups:", error)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deletePaymentGroup(id)
      await loadGroups()
    } catch (error) {
      console.error("Failed to delete payment group:", error)
      alert("Erro ao excluir grupo de pagamento. Por favor, tente novamente.")
    }
  }

  const getProgressBadge = (paid: number, total: number) => {
    const baseBadgeClass = "whitespace-nowrap px-2 py-0.5 text-xs font-semibold"
    if (paid === total) {
      return <Badge className={`${baseBadgeClass} bg-green-500 hover:bg-green-600`}>Concluído</Badge>
    }
    if (paid === 0) {
      return <Badge variant="secondary" className={baseBadgeClass}>Novo</Badge>
    }
    return <Badge className={`${baseBadgeClass} bg-blue-500 hover:bg-blue-600`}>Em Andamento</Badge>
  }

  return (
    <main className="sm:ml-14 p-4">
      <div className="flex flex-col gap-6">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-gray-800">Grupos de Pagamento</h1>
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
          <CardContent>
            <div className="flex flex-wrap gap-4 items-end mb-4 pt-4">
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
            </div>

            <div className="rounded-md border overflow-x-auto">
              <Table className="table-fixed w-full min-w-[1200px]">
                <colgroup>
                  <col className="w-[16%]" />
                  <col className="w-[12%]" />
                  <col className="w-[12%]" />
                  <col className="w-[10%]" />
                  <col className="w-[10%]" />
                  <col className="w-[12%]" />
                  <col className="w-[12%]" />
                  <col className="w-[10%]" />
                  <col className="w-[6%]" />
                </colgroup>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome do Grupo</TableHead>
                    <TableHead>Cliente</TableHead>
                    <TableHead>Documento Pagador</TableHead>
                    <TableHead className="text-center">Parcelas</TableHead>
                    <TableHead className="text-right">Valor Mensal</TableHead>
                    <TableHead className="text-right">Total Pago</TableHead>
                    <TableHead className="text-right">Total Restante</TableHead>
                    <TableHead className="text-center">Situação</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={9} className="h-24 text-center">
                        Carregando...
                      </TableCell>
                    </TableRow>
                  ) : groups.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={9} className="h-40">
                        <div className="flex flex-col items-center justify-center gap-4 text-muted-foreground">
                          <FolderOpen className="h-12 w-12 opacity-50" />
                          <div className="text-center">
                            <p className="font-medium">Nenhum grupo de pagamento encontrado</p>
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
                    groups.map((group) => (
                      <TableRow key={group.id} className="hover:bg-muted/50">
                        <TableCell className="font-medium">{group.groupName || `Grupo #${group.id}`}</TableCell>
                        <TableCell>{group.clientName}</TableCell>
                        <TableCell>{formatDisplayDocument(group.payerDocument)}</TableCell>
                        <TableCell className="text-center">
                          {group.paidInstallments} / {group.totalInstallments}
                        </TableCell>
                        <TableCell className="text-right">{formatDisplayCurrency(group.monthlyValue)}</TableCell>
                        <TableCell className="text-right">{formatDisplayCurrency(group.totalPaid)}</TableCell>
                        <TableCell className="text-right">{formatDisplayCurrency(group.totalRemaining)}</TableCell>
                        <TableCell className="text-center">
                          {getProgressBadge(group.paidInstallments, group.totalInstallments)}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center justify-end">
                            <AlertDialog>
                              <AlertDialogTrigger asChild>
                                <Button variant="ghost" size="sm" className="h-8 w-8 p-0 text-destructive hover:text-destructive">
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </AlertDialogTrigger>
                              <AlertDialogContent>
                                <AlertDialogHeader>
                                  <AlertDialogTitle>Excluir grupo de pagamento</AlertDialogTitle>
                                  <AlertDialogDescription>
                                    Tem certeza que deseja excluir o grupo &quot;{group.groupName || `Grupo #${group.id}`}&quot;?
                                    Todos os pagamentos e boletos associados serão excluídos permanentemente.
                                    Esta ação não pode ser desfeita.
                                  </AlertDialogDescription>
                                </AlertDialogHeader>
                                <AlertDialogFooter>
                                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                                  <AlertDialogAction
                                    onClick={() => handleDelete(group.id)}
                                    className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                  >
                                    Excluir
                                  </AlertDialogAction>
                                </AlertDialogFooter>
                              </AlertDialogContent>
                            </AlertDialog>
                          </div>
                        </TableCell>
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
