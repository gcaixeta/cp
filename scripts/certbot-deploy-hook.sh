#!/bin/bash
# Certbot deploy hook: executado automaticamente após renovação do cert.
# Instalar em: /etc/letsencrypt/renewal-hooks/deploy/cp-nginx.sh
set -e

PROJECT_DIR="/home/gustavorosa/projects/cp"
DOMAIN="cpacessoriaecobranca.com.br"
LETSENCRYPT_DIR="/etc/letsencrypt/live/${DOMAIN}"
SSL_DIR="${PROJECT_DIR}/nginx/ssl"

mkdir -p "$SSL_DIR"
cp "$LETSENCRYPT_DIR/fullchain.pem" "$SSL_DIR/"
cp "$LETSENCRYPT_DIR/privkey.pem"   "$SSL_DIR/"
chown -R $(stat -c '%U' "$PROJECT_DIR"):$(stat -c '%G' "$PROJECT_DIR") "$SSL_DIR"
chmod 600 "$SSL_DIR/privkey.pem"
chmod 644 "$SSL_DIR/fullchain.pem"

cd "$PROJECT_DIR"
docker compose restart nginx
