"use client"

import { useState } from "react"
import { Loader2, RefreshCw, Settings, Sparkles } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogTitle,
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
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { cn } from "@/lib/utils"
import { recalculateOverdueInterest } from "@/lib/api"

interface SettingsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

type SettingsSection = "sistema" | "geral"

const sections: { id: SettingsSection; label: string; icon: typeof Settings }[] = [
  { id: "sistema", label: "Sistema", icon: Settings },
  { id: "geral", label: "Geral", icon: Sparkles },
]

export function SettingsDialog({ open, onOpenChange }: SettingsDialogProps) {
  const [activeSection, setActiveSection] = useState<SettingsSection>("sistema")
  const [isRecalculating, setIsRecalculating] = useState(false)
  const [result, setResult] = useState<{ paymentsMarkedOverdue: number; paymentsRecalculated: number } | null>(null)

  const handleRecalculate = async () => {
    setIsRecalculating(true)
    setResult(null)
    try {
      const response = await recalculateOverdueInterest()
      setResult(response)
    } catch (error) {
      console.error("Failed to recalculate overdue interest:", error)
      alert("Erro ao recalcular juros. Por favor, tente novamente.")
    } finally {
      setIsRecalculating(false)
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next)
        if (!next) {
          setResult(null)
        }
      }}
    >
      <DialogContent className="flex h-[420px] max-w-2xl gap-0 overflow-hidden p-0 sm:max-w-2xl">
        <DialogTitle className="sr-only">Configurações</DialogTitle>

        <nav className="flex w-48 shrink-0 flex-col gap-1 border-r bg-muted/40 p-3">
          <h2 className="px-2 py-2 text-sm font-semibold">Configurações</h2>
          {sections.map((section) => (
            <button
              key={section.id}
              onClick={() => setActiveSection(section.id)}
              className={cn(
                "flex items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors",
                activeSection === section.id
                  ? "bg-background font-medium text-foreground shadow-sm"
                  : "text-muted-foreground hover:bg-background/60 hover:text-foreground"
              )}
            >
              <section.icon className="h-4 w-4" />
              {section.label}
            </button>
          ))}
        </nav>

        <div className="flex-1 overflow-y-auto p-6">
          {activeSection === "sistema" && (
            <div className="space-y-4">
              <div>
                <h3 className="text-base font-semibold">Sistema</h3>
                <p className="text-sm text-muted-foreground">
                  Ações administrativas sobre os dados do sistema.
                </p>
              </div>

              <div className="rounded-lg border p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium">Recalcular juros de pagamentos vencidos</p>
                    <p className="text-sm text-muted-foreground">
                      Marca pagamentos em atraso como vencidos e recalcula a multa e os juros
                      de todos os pagamentos vencidos, usando o mesmo cálculo do job diário.
                    </p>
                  </div>
                </div>

                <div className="mt-4 flex items-center gap-3">
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button size="sm" disabled={isRecalculating}>
                        {isRecalculating ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <RefreshCw className="h-4 w-4" />
                        )}
                        Recalcular juros
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Recalcular juros de pagamentos vencidos?</AlertDialogTitle>
                        <AlertDialogDescription>
                          Isso vai marcar pagamentos pendentes vencidos como &quot;Vencido&quot; e
                          recalcular o valor com multa e juros de todos os pagamentos vencidos.
                          Essa é a mesma operação executada automaticamente todos os dias.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancelar</AlertDialogCancel>
                        <AlertDialogAction onClick={handleRecalculate}>
                          Recalcular
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>

                  {result && (
                    <p className="text-sm text-muted-foreground">
                      {result.paymentsMarkedOverdue} marcado(s) como vencido, {" "}
                      {result.paymentsRecalculated} valor(es) recalculado(s).
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}

          {activeSection === "geral" && (
            <div className="space-y-1">
              <h3 className="text-base font-semibold">Geral</h3>
              <p className="text-sm text-muted-foreground">Mais opções em breve.</p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
