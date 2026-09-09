// Logger estruturado: cada chamada emite uma linha JSON no stdout do
// container Next.js, que é coletada pelo Promtail junto com os demais
// serviços (ver observability/promtail-config.yaml).

type LogLevel = "info" | "warn" | "error";

interface LogMeta {
  [key: string]: unknown;
}

function write(level: LogLevel, msg: string, meta?: LogMeta) {
  const line = JSON.stringify({
    app: "frontend",
    level,
    msg,
    timestamp: new Date().toISOString(),
    ...meta,
  });

  if (level === "error") {
    console.error(line);
  } else if (level === "warn") {
    console.warn(line);
  } else {
    console.log(line);
  }
}

export const logger = {
  info: (msg: string, meta?: LogMeta) => write("info", msg, meta),
  warn: (msg: string, meta?: LogMeta) => write("warn", msg, meta),
  error: (msg: string, meta?: LogMeta) => write("error", msg, meta),
};
