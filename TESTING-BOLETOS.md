# Guia de Testes - Geração de Boletos

Este documento explica como testar a funcionalidade de geração de boletos nos diferentes ambientes.

## 🎯 Modos de Teste Disponíveis

### 1. 🧪 Modo Mock (Recomendado para Desenvolvimento)

**Quando usar**: Desenvolvimento local diário, testes rápidos, CI/CD

**Características**:
- ⚡ Instantâneo (300-800ms simulados)
- 💰 Grátis (sem custos de API)
- 🔧 Não requer configuração
- ✅ Dados realistas mas fictícios

**Como ativar**:

```bash
# Opção 1: Via profile
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=local

# Opção 2: Via variável de ambiente
BANK_MOCK_ENABLED=true ./mvnw spring-boot:run
```

**Como identificar**: Nos logs você verá:
```
🧪 =====================================
🧪 MODO TESTE ATIVO - BOLETO MOCK
🧪 Payment ID: 123
🧪 Valor: R$ 100.00
🧪 Vencimento: 2026-03-15
🧪 =====================================
✅ Boleto MOCK gerado com sucesso
   📄 Nosso Número: MOCK-123456-123-ABC12345
   📊 Código de Barras: 077912345678901234567890123456789012345678
   💳 Linha Digitável: 07799.12345 67890.123456 78901.234567 8 9012345678901234
   🔗 PDF URL: https://mock-banco-inter.test/api/boleto/pdf/MOCK-...
```

---

### 2. 🏖️ Modo Sandbox (Testes de Integração)

**Quando usar**: Validar integração real, testes E2E, homologação

**Características**:
- 🔌 Integração real com API do Inter
- 🧪 Ambiente isolado de testes
- 🎫 Boletos de teste (não pagáveis)
- 🔐 Requer credenciais de sandbox

**Pré-requisitos**:

1. Conta no portal de desenvolvedores: https://developers.bancointer.com.br/
2. Aplicação de Sandbox configurada
3. Certificado digital de sandbox
4. Client ID e Secret de sandbox

**Como ativar**:

```bash
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=sandbox
```

**Configuração**:

Adicione ao arquivo `.env`:

```bash
INTER_SANDBOX_CLIENT_ID=seu_client_id_sandbox
INTER_SANDBOX_CLIENT_SECRET=seu_client_secret_sandbox
INTER_SANDBOX_CERTIFICATE_PATH=classpath:certs/inter-sandbox-cert.p12
INTER_SANDBOX_CERTIFICATE_PASSWORD=senha_do_certificado
```

Coloque o certificado em:
```
backend/src/main/resources/certs/inter-sandbox-cert.p12
```

**Como identificar**: Logs normais de produção, mas URL será sandbox:
```
🔐 Obtendo novo token OAuth2 do Banco Inter
📤 Gerando boleto no Banco Inter para payment 123
✅ Boleto gerado com sucesso para payment 123
```

---

### 3. 🚀 Modo Produção

**Quando usar**: Ambiente de produção com boletos reais

**Características**:
- 💳 Boletos reais e pagáveis
- 💰 Cobrado por transação
- 🔐 Máxima segurança

**Como ativar**:

```bash
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

---

## 🧪 Cenários de Teste

### Cenário 1: Criação de Payment Group com Boletos

1. Inicie o backend no modo desejado (mock/sandbox/prod)
2. Acesse o frontend: http://localhost:3000
3. Navegue para "Novo Grupo de Pagamentos"
4. Preencha o formulário:
   - Selecione um cliente
   - Defina pagador, valor e parcelas
   - ✅ **Marque** "Gerar boletos automaticamente"
5. Clique em "Criar Grupo"

**Resultado Esperado**:
- Payment Group criado com sucesso
- N boletos gerados (um para cada parcela)
- Status dos boletos: `GENERATED`

**Verificação via API**:

```bash
# Listar pagamentos do grupo
curl http://localhost:8080/v1/payments \
  -H "Authorization: Bearer {token}"

# Buscar boleto de um payment específico
curl http://localhost:8080/v1/boletos/payment/123 \
  -H "Authorization: Bearer {token}"
```

---

### Cenário 2: Geração Manual de Boleto

Para payments criados sem boleto:

```bash
curl -X POST http://localhost:8080/v1/boletos/payment/123/generate?bankType=INTER \
  -H "Authorization: Bearer {token}"
```

---

### Cenário 3: Retry de Boleto com Erro

Se um boleto falhou (status `ERROR`):

```bash
curl -X POST http://localhost:8080/v1/boletos/payment/123/retry?bankType=INTER \
  -H "Authorization: Bearer {token}"
```

---

## 📊 Tabela Comparativa

| Característica | Mock | Sandbox | Produção |
|----------------|------|---------|----------|
| **Velocidade** | ⚡⚡⚡ Instantâneo | 🐌 2-5s | 🐌 2-5s |
| **Certificado** | ❌ Não precisa | ✅ Sandbox | ✅ Produção |
| **Credenciais** | ❌ Não precisa | ✅ Sandbox | ✅ Produção |
| **Custo** | 💰 Grátis | 💰 Grátis | 💰💰 Pago |
| **Boleto Pagável** | ❌ Não | ❌ Não | ✅ Sim |
| **Teste OAuth2** | ❌ Não | ✅ Sim | ✅ Sim |
| **Teste SSL** | ❌ Não | ✅ Sim | ✅ Sim |
| **Ideal para** | Dev local | Testes E2E | Produção |

---

## 🔍 Validação dos Resultados

### Via Banco de Dados

```sql
-- Ver boletos gerados
SELECT 
    b.id,
    b.payment_id,
    b.bank_type,
    b.status,
    b.bank_boleto_id,
    b.created_at
FROM boletos b
ORDER BY b.created_at DESC
LIMIT 10;

-- Ver boletos com erro
SELECT * FROM boletos WHERE status = 'ERROR';

-- Ver boletos por status
SELECT status, COUNT(*) 
FROM boletos 
GROUP BY status;
```

### Via Logs

```bash
# Filtrar logs de geração de boletos
grep "BoletoService" logs/application.log

# Ver apenas modos mock
grep "🧪 MODO TESTE" logs/application.log

# Ver erros
grep "ERROR.*boleto" logs/application.log -i
```

### Via Frontend

No dialog de detalhes do pagamento, você verá:
- ✅ Código de barras
- ✅ Linha digitável
- ✅ Link para PDF (em mock será uma URL fictícia)
- ✅ Status do boleto

---

## 🐛 Troubleshooting

### Problema: "Modo mock não está ativando"

**Solução**:
```bash
# Verifique o profile ativo
grep "active" backend/src/main/resources/application*.yaml

# Force o mock via variável
BANK_MOCK_ENABLED=true ./mvnw spring-boot:run
```

### Problema: "Certificado não encontrado no sandbox"

**Solução**:
```bash
# Verifique se o arquivo existe
ls -la backend/src/main/resources/certs/

# Verifique permissões
chmod 644 backend/src/main/resources/certs/inter-sandbox-cert.p12
```

### Problema: "Erro 401 no sandbox"

**Solução**:
- Verifique se o Client ID e Secret estão corretos
- Confirme que são credenciais de **sandbox**, não de produção
- Verifique se o certificado corresponde à aplicação

---

## 📝 Checklist de Teste Completo

### Pré-Deploy

- [ ] Testes unitários passando
- [ ] Teste em modo Mock (local)
- [ ] Teste em modo Sandbox (integração)
- [ ] Validação de dados gerados
- [ ] Teste de retry em caso de erro
- [ ] Teste de geração manual
- [ ] Logs sem erros

### Homologação

- [ ] Deploy em ambiente de staging
- [ ] Usar modo Sandbox
- [ ] Gerar boletos de teste
- [ ] Validar com equipe de QA
- [ ] Testar diferentes cenários de erro
- [ ] Performance adequada (< 5s por boleto)

### Produção

- [ ] Credenciais de produção configuradas
- [ ] Certificado de produção instalado
- [ ] Variáveis de ambiente validadas
- [ ] Mock desabilitado (`bank.mock.enabled=false`)
- [ ] Monitoramento ativo
- [ ] Alertas configurados
- [ ] Backup do banco de dados
- [ ] Rollback plan preparado

---

## 🎓 Boas Práticas

1. **Desenvolvimento**: Use sempre modo Mock
2. **CI/CD**: Configure testes com Mock
3. **Homologação**: Use Sandbox antes de produção
4. **Produção**: Monitore logs e métricas
5. **Segurança**: Nunca commite certificados ou credenciais

---

## 📚 Recursos Adicionais

- [Documentação API Banco Inter](https://developers.bancointer.com.br/)
- [INTEGRACAO-BOLETOS.md](./INTEGRACAO-BOLETOS.md) - Documentação completa
- [SANDBOX-README.md](./backend/src/main/resources/certs/SANDBOX-README.md) - Guia de sandbox

---

**Última atualização**: 05/02/2026  
**Mantido por**: Equipe de Desenvolvimento
