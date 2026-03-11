# Certificados Sandbox - Banco Inter

Este guia explica como configurar o ambiente Sandbox para testes com a API real do Banco Inter.

## 📋 Pré-requisitos

1. Conta de desenvolvedor no Banco Inter
2. Credenciais de Sandbox (Client ID e Client Secret)
3. Certificado digital de Sandbox

## 🔑 Como Obter as Credenciais de Sandbox

### Passo 1: Acessar o Portal de Desenvolvedores

```
https://developers.bancointer.com.br/
```

### Passo 2: Criar Aplicação de Sandbox

1. Faça login no portal
2. Vá em "Minhas Aplicações"
3. Clique em "Nova Aplicação"
4. Selecione ambiente **"Sandbox"**
5. Configure os escopos necessários:
   - `boleto-cobranca.read`
   - `boleto-cobranca.write`

### Passo 3: Obter Credenciais

Após criar a aplicação, você receberá:
- **Client ID**: Identificador da sua aplicação
- **Client Secret**: Chave secreta
- **Certificado Digital**: Arquivo `.p12` para autenticação

### Passo 4: Download do Certificado

1. No portal, vá até sua aplicação
2. Clique em "Certificados"
3. Faça o download do certificado `.p12`
4. Anote a senha do certificado

## 📁 Estrutura de Arquivos

```
certs/
  ├── inter-cert.p12              # Certificado de PRODUÇÃO
  ├── inter-sandbox-cert.p12      # Certificado de SANDBOX (para testes)
  ├── README.md                   # Guia de produção
  └── SANDBOX-README.md           # Este arquivo
```

## ⚙️ Configuração

### 1. Colocar Certificado de Sandbox

```bash
# Copie o certificado baixado para o diretório correto
cp ~/Downloads/certificado-sandbox.p12 backend/src/main/resources/certs/inter-sandbox-cert.p12
```

### 2. Configurar Variáveis de Ambiente

Adicione ao seu arquivo `.env`:

```bash
# Sandbox Banco Inter
INTER_SANDBOX_CLIENT_ID=seu_client_id_de_sandbox
INTER_SANDBOX_CLIENT_SECRET=seu_client_secret_de_sandbox
INTER_SANDBOX_CERTIFICATE_PATH=classpath:certs/inter-sandbox-cert.p12
INTER_SANDBOX_CERTIFICATE_PASSWORD=senha_do_certificado_sandbox
```

### 3. Executar com Profile Sandbox

```bash
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=sandbox
```

## 🧪 Testando a Integração

### 1. Criar um Payment Group

No frontend, crie um novo grupo de pagamentos e marque a opção **"Gerar boletos automaticamente"**.

### 2. Verificar nos Logs

```
🔐 Obtendo novo token OAuth2 do Banco Inter
✅ Token OAuth2 obtido com sucesso. Válido por 3600 segundos
📤 Gerando boleto no Banco Inter para payment 123
✅ Boleto gerado com sucesso para payment 123
```

### 3. Consultar Boleto Gerado

```bash
# Via API
curl -X GET http://localhost:8080/v1/boletos/payment/123 \
  -H "Authorization: Bearer {seu_token}"
```

### 4. Verificar no Portal do Inter

Acesse o portal de desenvolvedores e verifique os boletos criados no ambiente Sandbox.

## 🆚 Diferenças: Mock vs Sandbox vs Produção

| Aspecto | Mock | Sandbox | Produção |
|---------|------|---------|----------|
| **Integração Real** | ❌ Não | ✅ Sim | ✅ Sim |
| **Certificado Necessário** | ❌ Não | ✅ Sim | ✅ Sim |
| **Boletos Válidos** | ❌ Não | ❌ Não | ✅ Sim |
| **Custo** | 💰 Grátis | 💰 Grátis | 💰 Pago |
| **Velocidade** | ⚡ Instantâneo | 🐌 Normal | 🐌 Normal |
| **Uso Recomendado** | Desenvolvimento local | Testes de integração | Produção |

## 🔄 Como Alternar Entre os Modos

### Modo Mock (Desenvolvimento Local)
```bash
./mvnw spring-boot:run -Dspring.profiles.active=local
# ou
BANK_MOCK_ENABLED=true ./mvnw spring-boot:run
```

### Modo Sandbox (Testes)
```bash
./mvnw spring-boot:run -Dspring.profiles.active=sandbox
```

### Modo Produção
```bash
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

## 📊 Monitoramento

### Logs Importantes

```bash
# Ver logs de boletos
grep "BoletoService" logs/application.log

# Ver chamadas à API
grep "InterBoletoStrategy" logs/application.log

# Ver erros
grep "ERROR" logs/application.log | grep -i boleto
```

## ⚠️ Limitações do Sandbox

- **Não gera boletos reais** pagáveis
- **Dados de teste** devem seguir padrões específicos
- **Rate limits** mais restritivos que produção
- **Webhooks** podem ter comportamento diferente

## 🐛 Troubleshooting

### Erro: "Certificado inválido"
```
Solução: Verifique se o certificado é realmente de sandbox e se a senha está correta
```

### Erro: "Client ID inválido"
```
Solução: Confirme que está usando as credenciais de SANDBOX, não as de produção
```

### Erro: "Timeout na API"
```
Solução: O sandbox pode estar instável. Tente novamente ou use o modo Mock
```

## 📚 Documentação Oficial

- [API Cobrança v3](https://developers.bancointer.com.br/reference/emissao-de-cobranca)
- [Autenticação OAuth2](https://developers.bancointer.com.br/docs/apis/autenticacao)
- [Certificados Digitais](https://developers.bancointer.com.br/docs/certificado-digital)

## 🎯 Checklist de Configuração

- [ ] Conta criada no portal de desenvolvedores
- [ ] Aplicação de Sandbox configurada
- [ ] Client ID e Secret obtidos
- [ ] Certificado `.p12` baixado
- [ ] Certificado colocado em `certs/inter-sandbox-cert.p12`
- [ ] Variáveis de ambiente configuradas no `.env`
- [ ] Profile `sandbox` testado com sucesso
- [ ] Logs verificados sem erros
- [ ] Boleto de teste gerado com sucesso

---

**Dica**: Use o modo Sandbox apenas quando precisar testar a integração real. 
Para desenvolvimento diário, o modo Mock é mais rápido e não requer configuração! 🚀
