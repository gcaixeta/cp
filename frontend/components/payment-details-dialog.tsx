"use client"

import { useState, useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { format } from "date-fns"
import { Loader2, PencilIcon, ChevronDownIcon } from "lucide-react"
import * as z from "zod"

import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
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
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { Badge } from "@/components/ui/badge"
import { updatePayment, type PaymentResponse } from "@/lib/api"
import { cn } from "@/lib/utils"
import { formatInputCurrency, formatDisplayPhone, formatDisplayCurrency } from "@/lib/format"

interface PaymentDetailsDialogProps {
  payment: PaymentResponse
  onSuccess: () => void
}

const formSchema = z.object({
  originalValue: z.string().min(1, "Valor é obrigatório"),
  dueDate: z.date(),
  paymentDate: z.date().optional().nullable(),
  observation: z.string().optional(),
})

export function PaymentDetailsDialog({ payment, onSuccess }: PaymentDetailsDialogProps) {
  const [open, setOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [dueDatePickerOpen, setDueDatePickerOpen] = useState(false)
  const [paymentDatePickerOpen, setPaymentDatePickerOpen] = useState(false)

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      originalValue: "",
      observation: "",
    },
  })

  // Load payment data when dialog opens
  useEffect(() => {
    if (open) {
      const formattedValue = payment.originalValue.toLocaleString("pt-BR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })

      // Parse dates as local time
      const [dyear, dmonth, dday] = payment.dueDate.split('-')
      const dueDate = new Date(parseInt(dyear), parseInt(dmonth) - 1, parseInt(dday))

      let paymentDate = null
      if (payment.paymentDate) {
        const [pyear, pmonth, pday] = payment.paymentDate.split('-')
        paymentDate = new Date(parseInt(pyear), parseInt(pmonth) - 1, parseInt(pday))
      }

      form.reset({
        originalValue: formattedValue,
        dueDate,
        paymentDate,
        observation: payment.observation || "",
      })
    }
  }, [open, payment, form])

  const getStatusBadge = (status: string) => {
    const badges = {
      PAID: <Badge variant="default" className="bg-green-500">Pago</Badge>,
      PAID_LATE: <Badge variant="default" className="bg-yellow-500">Pago com Atraso</Badge>,
      OVERDUE: <Badge variant="destructive">Atrasado</Badge>,
      PENDING: <Badge variant="secondary">Pendente</Badge>,
    }
    return badges[status as keyof typeof badges] || null
  }

  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    setIsSubmitting(true)

    try {
      // Parse currency value (1.234,56 -> 1234.56)
      const originalValue = parseFloat(
        values.originalValue.replace(/\./g, "").replace(",", ".")
      )

      // Format dates to ISO (YYYY-MM-DD)
      const dueDate = format(values.dueDate, "yyyy-MM-dd")
      const paymentDate = values.paymentDate
        ? format(values.paymentDate, "yyyy-MM-dd")
        : undefined

      await updatePayment(payment.id, {
        originalValue,
        dueDate,
        paymentDate,
        observation: values.observation || undefined,
      })

      // Success - close dialog and refresh
      setOpen(false)
      onSuccess()
    } catch (error) {
      console.error("Failed to update payment:", error)
      alert("Erro ao atualizar pagamento. Por favor, tente novamente.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
          <PencilIcon className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="min-w-3xl max-w-3xl">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <div>
              <DialogTitle>Detalhes do Pagamento</DialogTitle>
              <DialogDescription>
                Visualize e gerencie as informações deste lançamento
              </DialogDescription>
            </div>
            {getStatusBadge(payment.paymentStatus)}
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 py-4">
            {/* Reference Data */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Dados de Referência</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="space-y-1">
                  <FormLabel className="text-muted-foreground">Pagador</FormLabel>
                  <p className="text-sm font-medium truncate">{payment.payerName}</p>
                </div>

                <div className="space-y-1">
                  <FormLabel className="text-muted-foreground">Telefone</FormLabel>
                  <p className="text-sm font-medium">{formatDisplayPhone(payment.payerPhone)}</p>
                </div>

                <div className="space-y-1">
                  <FormLabel className="text-muted-foreground">Parcela</FormLabel>
                  <p className="text-sm font-medium">{payment.installmentNumber} / {payment.totalInstallments}</p>
                </div>

                <div className="space-y-1">
                  <FormLabel className="text-muted-foreground">Total com Juros</FormLabel>
                  <p className="text-sm font-medium">
                    {payment.overdueValue ? formatDisplayCurrency(payment.overdueValue) : "---"}
                  </p>
                </div>
              </div>
            </div>

            {/* Editable Fields */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Editar Pagamento</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <FormField
                  control={form.control}
                  name="originalValue"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Valor Original</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <span className="absolute left-3 top-2.5 text-muted-foreground">R$</span>
                          <Input
                            placeholder="0,00"
                            className="pl-10"
                            {...field}
                            onChange={(e) => field.onChange(formatInputCurrency(e.target.value))}
                          />
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="dueDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>Data de Vencimento</FormLabel>
                      <Popover open={dueDatePickerOpen} onOpenChange={setDueDatePickerOpen}>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button variant="outline" className={cn("w-full justify-between font-normal", !field.value && "text-muted-foreground")}>
                              {field.value ? format(field.value, "dd/MM/yyyy") : <span>Selecionar Data</span>}
                              <ChevronDownIcon className="h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            captionLayout="dropdown"
                            onSelect={(date) => { field.onChange(date); setDueDatePickerOpen(false); }}
                            fromYear={2020}
                            toYear={2030}
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="paymentDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>Data de Recebimento</FormLabel>
                      <Popover open={paymentDatePickerOpen} onOpenChange={setPaymentDatePickerOpen}>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button variant="outline" className={cn("w-full justify-between font-normal", !field.value && "text-muted-foreground")}>
                              {field.value ? format(field.value, "dd/MM/yyyy") : <span>Aguardando pagamento</span>}
                              <ChevronDownIcon className="h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value || undefined}
                            captionLayout="dropdown"
                            onSelect={(date) => { field.onChange(date); setPaymentDatePickerOpen(false); }}
                            fromYear={2020}
                            toYear={2030}
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>

            {/* Observation */}
            <FormField
              control={form.control}
              name="observation"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Observações</FormLabel>
                  <FormControl>
                    <Input placeholder="Observações sobre este pagamento..." {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </form>
        </Form>

        <DialogFooter>
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
            {isSubmitting ? "Salvando..." : "Salvar Alterações"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
