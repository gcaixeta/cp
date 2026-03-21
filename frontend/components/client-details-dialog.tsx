"use client"

import { useState, useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Loader2, PencilIcon, Trash2 } from "lucide-react"
import * as z from "zod"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { updateClient, deleteClient, type Client } from "@/lib/api"
import { formatDocument, formatPhone, formatInputPercentage } from "@/lib/format"

interface ClientDetailsDialogProps {
  client: Client
  onSuccess: () => void
}

const formSchema = z.object({
  clientName: z.string().min(3, "Nome deve ter no mínimo 3 caracteres").max(100, "Nome deve ter no máximo 100 caracteres"),
  document: z.string().min(11, "CPF/CNPJ inválido").max(18, "CPF/CNPJ inválido"),
  phone: z.string().optional(),
  address: z.string().min(5, "Endereço deve ter no mínimo 5 caracteres"),
  bank: z.string().optional(),
  lateFeeRate: z.string().optional(),
  monthlyInterestRate: z.string().optional(),
})

export function ClientDetailsDialog({ client, onSuccess }: ClientDetailsDialogProps) {
  const [open, setOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      clientName: "",
      document: "",
      phone: "",
      address: "",
      bank: "",
      lateFeeRate: "",
      monthlyInterestRate: "",
    },
  })

  useEffect(() => {
    if (open) {
      form.reset({
        clientName: client.name,
        document: formatDocument(client.document),
        phone: client.phone ? formatPhone(client.phone) : "",
        address: client.address,
        bank: client.bank || "",
        lateFeeRate: client.lateFeeRate ? (client.lateFeeRate * 100).toFixed(2).replace(".", ",") : "",
        monthlyInterestRate: client.monthlyInterestRate ? (client.monthlyInterestRate * 100).toFixed(2).replace(".", ",") : "",
      })
    }
  }, [open, client, form])

  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    setIsSubmitting(true)

    try {
      const lateFeeRate = values.lateFeeRate
        ? parseFloat(values.lateFeeRate.replace(",", ".")) / 100
        : undefined

      const monthlyInterestRate = values.monthlyInterestRate
        ? parseFloat(values.monthlyInterestRate.replace(",", ".")) / 100
        : undefined

      await updateClient(client.id, {
        clientName: values.clientName,
        document: values.document.replace(/\D/g, ""),
        phone: values.phone ? values.phone.replace(/\D/g, "") : undefined,
        address: values.address,
        bank: values.bank || undefined,
        lateFeeRate,
        monthlyInterestRate,
      })

      setOpen(false)
      onSuccess()
    } catch (error) {
      console.error("Failed to update client:", error)
      alert("Erro ao atualizar cliente. Por favor, tente novamente.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async () => {
    setIsDeleting(true)
    try {
      await deleteClient(client.id)
      setShowDeleteDialog(false)
      setOpen(false)
      onSuccess()
    } catch (error) {
      console.error("Failed to delete client:", error)
      alert("Erro ao deletar cliente. Por favor, tente novamente.")
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger asChild>
          <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
            <PencilIcon className="h-4 w-4" />
          </Button>
        </DialogTrigger>
        <DialogContent className="min-w-3xl max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Detalhes do Cliente</DialogTitle>
            <DialogDescription>
              Visualize e edite as informações do cliente
            </DialogDescription>
          </DialogHeader>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 py-4">
              {/* Dados Básicos */}
              <div className="space-y-4">
                <h3 className="text-lg font-medium">Dados Básicos</h3>

                <FormField
                  control={form.control}
                  name="clientName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nome do Cliente</FormLabel>
                      <FormControl>
                        <Input placeholder="Nome completo" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="document"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>CPF/CNPJ</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="000.000.000-00"
                            {...field}
                            onChange={(e) => {
                              const formatted = formatDocument(e.target.value)
                              field.onChange(formatted)
                            }}
                            maxLength={18}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="phone"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Telefone</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="(00) 00000-0000"
                            {...field}
                            onChange={(e) => {
                              const formatted = formatPhone(e.target.value)
                              field.onChange(formatted)
                            }}
                            maxLength={15}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </div>

              {/* Endereço e Banco */}
              <div className="space-y-4 pt-2">
                <h3 className="text-lg font-medium">Endereço e Banco</h3>

                <FormField
                  control={form.control}
                  name="address"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Endereço</FormLabel>
                      <FormControl>
                        <Input placeholder="Endereço completo" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="bank"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Banco</FormLabel>
                      <FormControl>
                        <Input placeholder="Nome do banco" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Taxas Padrão */}
              <div className="space-y-4 pt-2">
                <h3 className="text-lg font-medium">Taxas Padrão</h3>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="lateFeeRate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Taxa de Multa (%)</FormLabel>
                        <FormControl>
                          <div className="relative">
                            <Input
                              placeholder="0,00"
                              {...field}
                              onChange={(e) => {
                                const formatted = formatInputPercentage(e.target.value)
                                field.onChange(formatted)
                              }}
                            />
                            <span className="absolute right-3 top-2.5 text-muted-foreground text-sm">
                              %
                            </span>
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="monthlyInterestRate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Juros Mensal (%)</FormLabel>
                        <FormControl>
                          <div className="relative">
                            <Input
                              placeholder="0,00"
                              {...field}
                              onChange={(e) => {
                                const formatted = formatInputPercentage(e.target.value)
                                field.onChange(formatted)
                              }}
                            />
                            <span className="absolute right-3 top-2.5 text-muted-foreground text-sm">
                              %
                            </span>
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </div>
            </form>
          </Form>

          <DialogFooter className="mt-6 flex justify-between sm:justify-between">
            <Button
              type="button"
              variant="destructive"
              onClick={() => setShowDeleteDialog(true)}
              disabled={isSubmitting}
              className="mr-auto"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              Deletar
            </Button>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setOpen(false)}
                disabled={isSubmitting}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                onClick={form.handleSubmit(onSubmit)}
                disabled={isSubmitting}
              >
                {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isSubmitting ? "Salvando..." : "Salvar"}
              </Button>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Tem certeza que deseja excluir este cliente?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta ação não pode ser desfeita. Isso irá deletar permanentemente o cliente
              e todos os dados associados a ele.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={isDeleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDeleting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {isDeleting ? "Deletando..." : "Confirmar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
