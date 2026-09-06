"use client"

import { ColumnDef, Column } from "@tanstack/react-table"
import { ArrowUp, ArrowDown, ArrowUpDown, ChevronDown, ChevronRight, AlertCircle, Check } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { PaymentDetailsDialog } from "@/components/payment-details-dialog"
import { formatDisplayCurrency } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { PaymentResponse } from "@/lib/api"

export type PaymentRow = PaymentResponse & { subRows?: PaymentResponse[] }

export const formatDate = (dateString: string) => {
  // Parse date as local time to avoid timezone issues
  const [year, month, day] = dateString.split('-')
  const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day))
  return date.toLocaleDateString("pt-BR")
}

export const getStatusBadge = (status: string) => {
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

function SortableHeader({ label, column }: { label: string; column: Column<PaymentRow, unknown> }) {
  const sorted = column.getIsSorted()
  const Icon = sorted === "asc" ? ArrowUp : sorted === "desc" ? ArrowDown : ArrowUpDown
  return (
    <button
      type="button"
      onClick={() => column.toggleSorting(sorted === "asc")}
      className="inline-flex items-center gap-1 hover:text-foreground"
    >
      {label}
      <Icon className={cn("h-3 w-3", !sorted && "opacity-40")} />
    </button>
  )
}

export interface PaymentColumnHandlers {
  onMarkAsPaid: (paymentId: number) => void
  onRefresh: () => void
}

export function getPaymentColumns(handlers: PaymentColumnHandlers): ColumnDef<PaymentRow>[] {
  return [
    {
      id: "expander",
      header: () => null,
      cell: ({ row }) =>
        row.getCanExpand() ? (
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={row.getToggleExpandedHandler()}
          >
            {row.getIsExpanded() ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </Button>
        ) : null,
    },
    {
      accessorKey: "payerName",
      header: ({ column }) => <SortableHeader label="Pagador" column={column} />,
      cell: ({ row }) =>
        row.depth === 0 ? (
          <div className="flex flex-col">
            <span className="font-medium">{row.original.payerName}</span>
          </div>
        ) : (
          <span className="pl-8 text-xs text-muted-foreground italic">
            Parcela atrasada do grupo
          </span>
        ),
    },
    {
      id: "installment",
      header: () => <div className="text-center">Parcela</div>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className={cn("text-center", row.depth > 0 && "text-xs")}>
          {row.original.installmentNumber} / {row.original.totalInstallments}
        </div>
      ),
    },
    {
      accessorKey: "dueDate",
      header: ({ column }) => <SortableHeader label="Vencimento" column={column} />,
      cell: ({ row }) => (
        <span className={cn(row.depth > 0 && "text-xs")}>{formatDate(row.original.dueDate)}</span>
      ),
    },
    {
      accessorKey: "paymentDate",
      header: ({ column }) => <SortableHeader label="Data Pagamento" column={column} />,
      sortingFn: (rowA, rowB) => {
        const a = rowA.original.paymentDate
        const b = rowB.original.paymentDate
        const at = a ? new Date(a).getTime() : Infinity
        const bt = b ? new Date(b).getTime() : Infinity
        return at - bt
      },
      cell: ({ row }) => (
        <span className={cn(row.depth > 0 && "text-xs")}>
          {row.original.paymentDate ? formatDate(row.original.paymentDate) : "---"}
        </span>
      ),
    },
    {
      accessorKey: "originalValue",
      header: ({ column }) => (
        <div className="text-right">
          <SortableHeader label="Valor Original" column={column} />
        </div>
      ),
      cell: ({ row }) => (
        <div className={cn("text-right", row.depth > 0 && "text-xs")}>
          {formatDisplayCurrency(row.original.originalValue)}
        </div>
      ),
    },
    {
      id: "overdueValue",
      header: () => <div className="text-right">Valor Com Juros</div>,
      enableSorting: false,
      cell: ({ row }) => {
        const payment = row.original
        const valueLabel = payment.overdueValue ? formatDisplayCurrency(payment.overdueValue) : "---"
        if (row.depth > 0) {
          return (
            <div className="text-right text-xs">
              <span
                className={
                  payment.paymentStatus === "PAID_LATE"
                    ? "font-medium text-yellow-700"
                    : payment.paymentStatus === "OVERDUE"
                    ? "font-medium text-destructive"
                    : ""
                }
              >
                {valueLabel}
              </span>
            </div>
          )
        }
        return (
          <div className="flex flex-col items-end text-right">
            <span>{valueLabel}</span>
            {(payment.subRows?.length ?? 0) > 0 && (
              <span className="text-[10px] text-orange-600 flex items-center gap-0.5 font-bold">
                <AlertCircle className="h-3 w-3" />
                Possui parcelas atrasadas
              </span>
            )}
          </div>
        )
      },
    },
    {
      id: "status",
      header: () => <div className="text-center">Status</div>,
      enableSorting: false,
      cell: ({ row }) => <div className="text-center">{getStatusBadge(row.original.paymentStatus)}</div>,
    },
    {
      id: "actions",
      header: () => null,
      enableSorting: false,
      cell: ({ row }) => {
        const payment = row.original
        return (
          <div className="flex items-center gap-2">
            <PaymentDetailsDialog payment={payment} onSuccess={handlers.onRefresh} />
            {payment.paymentStatus !== "PAID" && payment.paymentStatus !== "PAID_LATE" && (
              <Button
                size="sm"
                variant="outline"
                className={cn("gap-1", row.depth > 0 ? "h-7 text-xs" : "h-8")}
                onClick={() => handlers.onMarkAsPaid(payment.id)}
              >
                <Check className="h-3 w-3" />
                Pagar
              </Button>
            )}
          </div>
        )
      },
    },
  ]
}
