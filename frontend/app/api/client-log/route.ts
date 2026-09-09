import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";

// Recebe erros não tratados capturados no navegador (ver components/client-error-listener.tsx)
// e os registra no stdout do servidor, para que cheguem ao Loki como os demais logs.
export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => null);

  if (!body || typeof body.message !== "string") {
    return NextResponse.json({ error: "invalid payload" }, { status: 400 });
  }

  logger.error("client_error", {
    message: body.message,
    stack: typeof body.stack === "string" ? body.stack : undefined,
    url: typeof body.url === "string" ? body.url : undefined,
    userAgent: request.headers.get("user-agent") ?? undefined,
  });

  return NextResponse.json({ ok: true });
}
