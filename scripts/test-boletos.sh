#!/bin/bash

# Script para testar geração de boletos em diferentes modos
# Uso: ./scripts/test-boletos.sh [mock|sandbox|prod]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para imprimir mensagens coloridas
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Banner
show_banner() {
    echo ""
    echo "╔════════════════════════════════════════════════╗"
    echo "║   🎫 Teste de Geração de Boletos - CP System  ║"
    echo "╚════════════════════════════════════════════════╝"
    echo ""
}

# Mostrar ajuda
show_help() {
    echo "Uso: $0 [modo]"
    echo ""
    echo "Modos disponíveis:"
    echo "  mock      - Teste com estratégia Mock (rápido, sem certificados)"
    echo "  sandbox   - Teste com API Sandbox do Banco Inter (integração real)"
    echo "  prod      - Teste com API de Produção (boletos reais)"
    echo ""
    echo "Exemplos:"
    echo "  $0 mock     # Inicia em modo Mock"
    echo "  $0 sandbox  # Inicia em modo Sandbox"
    echo "  $0 prod     # Inicia em modo Produção"
    echo ""
}

# Verificar modo
MODE=${1:-mock}

if [ "$MODE" != "mock" ] && [ "$MODE" != "sandbox" ] && [ "$MODE" != "prod" ]; then
    log_error "Modo inválido: $MODE"
    show_help
    exit 1
fi

show_banner

# Determinar profile baseado no modo
case "$MODE" in
    mock)
        PROFILE="local"
        log_info "Modo: 🧪 MOCK (Testes locais sem certificados)"
        log_warning "Este modo NÃO gera boletos reais"
        ;;
    sandbox)
        PROFILE="sandbox"
        log_info "Modo: 🏖️  SANDBOX (Integração real - ambiente de testes)"
        log_warning "Requer certificado e credenciais de sandbox"
        ;;
    prod)
        PROFILE="prod"
        log_info "Modo: 🚀 PRODUÇÃO (Boletos reais e pagáveis)"
        log_error "CUIDADO: Este modo gera boletos REAIS!"
        read -p "Tem certeza que deseja continuar? (yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            log_info "Cancelado pelo usuário"
            exit 0
        fi
        ;;
esac

echo ""
log_info "Profile Spring Boot: $PROFILE"
echo ""

# Verificar se o backend existe
if [ ! -d "$BACKEND_DIR" ]; then
    log_error "Diretório backend não encontrado: $BACKEND_DIR"
    exit 1
fi

# Verificar certificados se não for mock
if [ "$MODE" = "sandbox" ]; then
    CERT_PATH="$BACKEND_DIR/src/main/resources/certs/inter-sandbox-cert.p12"
    if [ ! -f "$CERT_PATH" ]; then
        log_warning "Certificado de sandbox não encontrado:"
        log_warning "  $CERT_PATH"
        log_info "Veja: backend/src/main/resources/certs/SANDBOX-README.md"
        echo ""
        read -p "Continuar mesmo assim? (yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            exit 0
        fi
    else
        log_success "Certificado de sandbox encontrado"
    fi
elif [ "$MODE" = "prod" ]; then
    CERT_PATH="$BACKEND_DIR/src/main/resources/certs/inter-cert.p12"
    if [ ! -f "$CERT_PATH" ]; then
        log_error "Certificado de produção não encontrado:"
        log_error "  $CERT_PATH"
        log_info "Veja: backend/src/main/resources/certs/README.md"
        exit 1
    else
        log_success "Certificado de produção encontrado"
    fi
fi

# Verificar .env se não for mock
if [ "$MODE" != "mock" ]; then
    ENV_FILE="$PROJECT_ROOT/.env"
    if [ ! -f "$ENV_FILE" ]; then
        log_warning "Arquivo .env não encontrado: $ENV_FILE"
        log_info "Certifique-se de configurar as variáveis de ambiente"
    else
        log_success "Arquivo .env encontrado"
    fi
fi

echo ""
log_info "═══════════════════════════════════════════════"
log_info "Iniciando backend em modo: $MODE"
log_info "═══════════════════════════════════════════════"
echo ""

# Ir para o diretório do backend
cd "$BACKEND_DIR"

# Iniciar o backend com o profile correto
log_info "Executando: ./mvnw spring-boot:run -Dspring.profiles.active=$PROFILE"
echo ""

./mvnw spring-boot:run -Dspring.profiles.active=$PROFILE

log_success "Backend finalizado"
