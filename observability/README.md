# Observabilidade — Logs centralizados

Stack: **Grafana Loki** (armazenamento/índice) + **Promtail** (coleta) + **Grafana** (consulta).

O Promtail lê o log de todo container Docker automaticamente (via `docker.sock`), então
qualquer serviço que escreva no stdout/stderr já é coletado — não é necessário instrumentar
nada extra ao adicionar um novo serviço ao `docker-compose.yml`.

## Subir a stack

```bash
docker-compose up -d loki promtail grafana
```

## Acessar o Grafana

O Grafana **não é exposto publicamente** — só em `127.0.0.1:3001` na VPS. Acesse via túnel SSH:

```bash
ssh -L 3001:localhost:3001 usuario@vps
```

Depois abra `http://localhost:3001` no navegador. Login: `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` (definidos no `.env`).

## Configurar o datasource (primeira vez)

Connections → Data sources → Add data source → Loki → URL: `http://loki:3100` → Save & test.

## Queries úteis (LogQL)

```logql
{service="backend"}                  # todos os logs do backend
{service="nginx"}                    # access/error log do Nginx
{service="frontend"}                 # logs do Next.js (server-side + erros client-side)
{service="backend"} |= "ERROR"       # só erros do backend
{service="backend"} | json | level="ERROR"   # filtra pelo campo JSON `level`
```

## Retenção

Controlada por `LOKI_RETENTION_DAYS` no `.env` (padrão 30 dias). A compactação/expurgo roda
automaticamente via o componente `compactor` do Loki.
