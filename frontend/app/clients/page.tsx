"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Card, CardContent } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Plus, Users } from "lucide-react"
import { fetchAllClients, type Client } from "@/lib/api"
import { ClientDetailsDialog } from "@/components/client-details-dialog"
import { formatDisplayDocument, formatDisplayPhone, formatDisplayPercentage } from "@/lib/format"

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadClients()
  }, [])

  const loadClients = async () => {
    try {
      setLoading(true)
      const data = await fetchAllClients()
      setClients(data)
    } catch (error) {
      console.error("Failed to load clients:", error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="sm:ml-14 p-4">
      <div className="flex flex-col gap-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div className="flex items-center gap-4">
            <h1 className="text-2xl font-bold text-gray-800">Clientes</h1>
            <Link href="/clients/new">
              <Button size="sm" className="gap-2">
                <Plus className="h-4 w-4" />
                Novo Cliente
              </Button>
            </Link>
          </div>
        </div>

        <Card>
          <CardContent>
            <div className="rounded-md border overflow-x-auto">
              <Table className="table-fixed w-full min-w-[1000px]">
                <colgroup>
                  <col className="w-[22%]" />
                  <col className="w-[16%]" />
                  <col className="w-[14%]" />
                  <col className="w-[16%]" />
                  <col className="w-[10%]" />
                  <col className="w-[10%]" />
                  <col className="w-[12%]" />
                </colgroup>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome</TableHead>
                    <TableHead>Documento</TableHead>
                    <TableHead>Telefone</TableHead>
                    <TableHead>Banco</TableHead>
                    <TableHead className="text-center">Taxa Multa</TableHead>
                    <TableHead className="text-center">Juros Mensal</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={7} className="h-24 text-center">
                        Carregando clientes...
                      </TableCell>
                    </TableRow>
                  ) : clients.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="h-40">
                        <div className="flex flex-col items-center justify-center gap-4 text-muted-foreground">
                          <Users className="h-12 w-12 opacity-50" />
                          <div className="text-center">
                            <p className="font-medium">Nenhum cliente cadastrado</p>
                            <p className="text-sm">Cadastre seu primeiro cliente para começar</p>
                          </div>
                          <Link href="/clients/new">
                            <Button className="gap-2">
                              <Plus className="h-4 w-4" />
                              Novo Cliente
                            </Button>
                          </Link>
                        </div>
                      </TableCell>
                    </TableRow>
                  ) : (
                    clients.map((client) => (
                      <TableRow key={client.id} className="hover:bg-muted/50">
                        <TableCell className="font-medium">{client.name}</TableCell>
                        <TableCell>{formatDisplayDocument(client.document)}</TableCell>
                        <TableCell>{formatDisplayPhone(client.phone)}</TableCell>
                        <TableCell>{client.bank || "---"}</TableCell>
                        <TableCell className="text-center">{formatDisplayPercentage(client.lateFeeRate)}</TableCell>
                        <TableCell className="text-center">{formatDisplayPercentage(client.monthlyInterestRate)}</TableCell>
                        <TableCell>
                          <div className="flex items-center justify-end gap-2">
                            <ClientDetailsDialog client={client} onSuccess={loadClients} />
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
