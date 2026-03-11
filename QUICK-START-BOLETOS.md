# 🚀 Quick Start - Geração de Boletos

Guia rápido para começar a testar a geração de boletos em 5 minutos!

## ⚡ TL;DR - Testar Agora

```bash
# 1. Iniciar backend em modo Mock (sem configuração)
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=local

# Ou use o script helper:
./scripts/test-boletos.sh mock
```

```bash
# 2. Em outro terminal, iniciar frontend
cd frontend
npm run dev
```

```bash
# 3. Acesse http://localhost:3000
# - Faça login
# - Crie um novo grupo de pagamentos
# - ✅ Marque "Gerar boletos automaticamente"
# - Pronto! Boletos serão gerados instantaneamente
```

## 🎯 Os 3 Modos de Teste

### 1. 🧪 Mock (Desenvolvimento Local)

**Mais rápido e sem configuração**

```bash
# Iniciar
./scripts/test-boletos.sh mock

# Ou manualmente
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=local
```

**Características:**
- ⚡ Instantâneo (300-800ms)
- 🚫 Sem certificados
- 🚫 Sem credenciais
- ✅ Dados realistas
- ✅ Ideal para desenvolvimento

**Logs esperados:**
```
🧪 =====================================
🧪 MODO TESTE ATIVO - BOLETO MOCK
🧪 Payment ID: 123
🧪 Valor: R$ 100.00
✅ Boleto MOCK gerado com sucesso
   📄 Nosso Número: MOCK-123456-123-ABC12345
```

---

### 2. 🏖️ Sandbox (Testes de Integração)

**API real do Banco Inter em ambiente de testes**

```bash
# 1. Configure as credenciais
cp .env.example .env
# Edite o .env com suas credenciais de sandbox

# 2. Coloque o certificado
# backend/src/main/resources/certs/inter-sandbox-cert.p12

# 3. Iniciar
./scripts/test-boletos.sh sandbox
```

**Onde obter credenciais:**
- Portal: https://developers.bancointer.com.br/
- Guia completo: `backend/src/main/resources/certs/SANDBOX-README.md`

**Variáveis necessárias no `.env`:**
```bash
INTER_SANDBOX_CLIENT_ID=seu_client_id
INTER_SANDBOX_CLIENT_SECRET=seu_client_secret
INTER_SANDBOX_CERTIFICATE_PATH=classpath:certs/inter-sandbox-cert.p12
INTER_SANDBOX_CERTIFICATE_PASSWORD=senha_certificado
```

---

### 3. 🚀 Produção (Boletos Reais)

```bash
# ⚠️ CUIDADO: Gera boletos REAIS!
./scripts/test-boletos.sh prod
```

## 📋 Checklist Rápido

### Para Modo Mock (5 minutos)
- [ ] Clone o repositório
- [ ] `cd backend && ./mvnw spring-boot:run -Dspring.profiles.active=local`
- [ ] `cd frontend && npm run dev`
- [ ] Crie um payment group com checkbox marcado
- [ ] ✅ Pronto!

### Para Modo Sandbox (15 minutos)
- [ ] Crie conta em https://developers.bancointer.com.br/
- [ ] Crie aplicação de Sandbox
- [ ] Baixe certificado e credenciais
- [ ] Configure `.env` com credenciais
- [ ] Coloque certificado em `certs/inter-sandbox-cert.p12`
- [ ] `./scripts/test-boletos.sh sandbox`
- [ ] Teste a geração de boletos

### Para Produção (30 minutos)
- [ ] Obtenha credenciais de produção
- [ ] Baixe certificado de produção
- [ ] Configure `.env` de produção
- [ ] Teste em sandbox primeiro!
- [ ] Deploy em produção
- [ ] Monitore logs

## 🔍 Como Verificar se Funcionou

### Via Frontend
1. Crie um payment group
2. Marque "Gerar boletos automaticamente"
3. Após criar, vá em "Pagamentos"
4. Clique em um pagamento
5. Veja os detalhes do boleto:
   - Código de barras
   - Linha digitável
   - Link do PDF (em mock será fictício)

### Via API
```bash
# Listar pagamentos
curl http://localhost:8080/v1/payments \
  -H "Authorization: Bearer {seu_token}"

# Buscar boleto
curl http://localhost:8080/v1/boletos/payment/123 \
  -H "Authorization: Bearer {seu_token}"
```

### Via Banco de Dados
```sql
-- Ver últimos boletos gerados
SELECT 
    id,
    payment_id,
    bank_type,
    status,
    bank_boleto_id,
    created_at
FROM boletos
ORDER BY created_at DESC
LIMIT 10;
```

### Via Logs
```bash
# Ver logs em tempo real
tail -f logs/application.log | grep -i boleto

# Ver apenas modo mock
grep "🧪 MODO TESTE" logs/application.log

# Ver erros
grep "ERROR.*boleto" logs/application.log -i
```

## 🎨 Exemplo de Resposta do Boleto

```json
{
  "id": 1,
  "paymentId": 123,
  "bankType": "INTER",
  "bankBoletoId": "MOCK-123456-123-ABC12345",
  "barcode": "077912345678901234567890123456789012345678",
  "digitableLine": "07799.12345 67890.123456 78901.234567 8 9012345678901234",
  "pdfUrl": "https://mock-banco-inter.test/api/boleto/pdf/MOCK-123456-123-ABC12345",
  "status": "GENERATED",
  "errorMessage": null,
  "createdAt": "2026-02-05T10:30:00"
}
```

## 🐛 Problemas Comuns

### "Certificado não encontrado"
```bash
# Mock não precisa de certificado
./scripts/test-boletos.sh mock

# Para sandbox/prod, verifique:
ls -la backend/src/main/resources/certs/
```

### "Erro 401 Unauthorized"
```bash
# Verifique suas credenciais no .env
cat .env | grep INTER

# Confirme que são credenciais de SANDBOX se usando sandbox
```

### "Modo mock não está ativando"
```bash
# Force o modo mock
BANK_MOCK_ENABLED=true ./mvnw spring-boot:run
```

### "Boleto com status ERROR"
```bash
# Tente novamente (retry)
curl -X POST http://localhost:8080/v1/boletos/payment/123/retry?bankType=INTER \
  -H "Authorization: Bearer {token}"
```

## 📚 Documentação Completa

- [INTEGRACAO-BOLETOS.md](./INTEGRACAO-BOLETOS.md) - Documentação técnica completa
- [TESTING-BOLETOS.md](./TESTING-BOLETOS.md) - Guia detalhado de testes
- [SANDBOX-README.md](./backend/src/main/resources/certs/SANDBOX-README.md) - Configuração sandbox

## 🆘 Precisa de Ajuda?

1. Verifique os logs: `tail -f logs/application.log`
2. Consulte a documentação completa
3. Use o modo Mock primeiro
4. Teste no Sandbox antes de produção

---

**Dica**: Para desenvolvimento diário, sempre use o modo Mock! 🚀

É rápido, não requer configuração e funciona perfeitamente para testes locais.
