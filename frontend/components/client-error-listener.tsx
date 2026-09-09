"use client";

import { useEffect } from "react";

function reportError(message: string, stack?: string) {
  fetch("/api/client-log", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message, stack, url: window.location.href }),
    keepalive: true,
  }).catch(() => {
    // Se o report falhar, não há para onde reportar o erro do report.
  });
}

// Captura erros não tratados no navegador e os envia para /api/client-log,
// que os registra no servidor (e dali para o Loki).
export function ClientErrorListener() {
  useEffect(() => {
    function handleError(event: ErrorEvent) {
      reportError(event.message, event.error?.stack);
    }

    function handleRejection(event: PromiseRejectionEvent) {
      const reason = event.reason;
      reportError(
        reason instanceof Error ? reason.message : String(reason),
        reason instanceof Error ? reason.stack : undefined
      );
    }

    window.addEventListener("error", handleError);
    window.addEventListener("unhandledrejection", handleRejection);
    return () => {
      window.removeEventListener("error", handleError);
      window.removeEventListener("unhandledrejection", handleRejection);
    };
  }, []);

  return null;
}
