# Certificados Banco Inter

Este diretório deve conter o certificado digital necessário para integração com a API do Banco Inter.

## Estrutura Esperada

```
certs/
  ├── inter-cert.p12  # Certificado digital do Banco Inter (formato PKCS12)
  └── README.md       # Este arquivo
```

## Como Obter o Certificado

1. Acesse o portal do Banco Inter para desenvolvedores
2. Faça o download do certificado digital em formato `.p12` (PKCS12)
3. Coloque o arquivo neste diretório com o nome `inter-cert.p12`
4. Configure a senha do certificado na variável de ambiente `INTER_CERTIFICATE_PASSWORD`

## Configuração das Variáveis de Ambiente

Adicione as seguintes variáveis ao seu arquivo `.env`:

```bash
INTER_CLIENT_ID=seu_client_id_aqui
INTER_CLIENT_SECRET=seu_client_secret_aqui
INTER_CERTIFICATE_PATH=classpath:certs/inter-cert.p12
INTER_CERTIFICATE_PASSWORD=senha_do_certificado
```

## Segurança

⚠️ **IMPORTANTE**: 
- Nunca commite o certificado digital no repositório Git
- O certificado contém informações sensíveis e deve ser tratado como uma credencial
- O arquivo `.gitignore` já está configurado para ignorar arquivos `.p12` neste diretório
- Em produção, armazene o certificado em um local seguro (ex: secrets manager)

## Desenvolvimento Local

Para desenvolvimento local sem integração real com o Banco Inter:
- A aplicação funcionará normalmente sem o certificado
- As chamadas à API do Banco Inter resultarão em erro
- O sistema marcará os boletos como `ERROR` e permitirá retry posteriormente
