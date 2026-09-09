#!/bin/bash

# Script para copiar certificados SSL do Let's Encrypt para o projeto
# Uso: ./scripts/copy-ssl-certs.sh

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configurações
DOMAIN="cpassessoriaecobranca.com"
LETSENCRYPT_DIR="/etc/letsencrypt/live/${DOMAIN}"
PROJECT_SSL_DIR="$(dirname "$0")/../nginx/ssl"

echo -e "${YELLOW}=== Copiando Certificados SSL ===${NC}"
echo ""

# Verificar se o diretório do Let's Encrypt existe
# (usa sudo pois /etc/letsencrypt/live é acessível apenas por root)
if ! sudo test -d "$LETSENCRYPT_DIR"; then
    echo -e "${RED}Erro: Diretório do Let's Encrypt não encontrado: $LETSENCRYPT_DIR${NC}"
    echo "Certifique-se de que os certificados estão instalados corretamente."
    exit 1
fi

# Criar diretório SSL no projeto se não existir
mkdir -p "$PROJECT_SSL_DIR"

# Copiar certificados
echo "Copiando fullchain.pem..."
sudo cp "$LETSENCRYPT_DIR/fullchain.pem" "$PROJECT_SSL_DIR/"

echo "Copiando privkey.pem..."
sudo cp "$LETSENCRYPT_DIR/privkey.pem" "$PROJECT_SSL_DIR/"

# Ajustar propriedade dos arquivos
echo "Ajustando permissões..."
sudo chown -R $USER:$USER "$PROJECT_SSL_DIR"
chmod 600 "$PROJECT_SSL_DIR/privkey.pem"
chmod 644 "$PROJECT_SSL_DIR/fullchain.pem"

echo ""
echo -e "${GREEN}✓ Certificados copiados com sucesso!${NC}"
echo ""
echo "Arquivos criados:"
ls -lh "$PROJECT_SSL_DIR"
echo ""
echo -e "${YELLOW}Próximos passos:${NC}"
echo "1. Reiniciar o container Nginx:"
echo "   docker compose restart nginx"
echo ""
echo "2. Ou fazer rebuild completo:"
echo "   docker compose down && docker compose up -d --build"
echo ""
echo -e "${YELLOW}Nota:${NC} Os certificados Let's Encrypt expiram a cada 90 dias."
echo "Execute este script novamente após renovar os certificados."
