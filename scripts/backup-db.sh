#!/bin/bash

#############################################
# CPSystem - PostgreSQL Backup to AWS S3
# 
# Este script faz backup do banco de dados
# PostgreSQL e envia para o S3
#############################################

set -e  # Exit on error

# ==========================================
# CARREGAR VARIÁVEIS DE AMBIENTE
# ==========================================

# Determinar o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Carregar arquivo .env se existir
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "[INFO] Carregando variáveis de $PROJECT_ROOT/.env"
    # Carregar apenas variáveis específicas necessárias para backup
    export $(grep -E "^(POSTGRES_|DB_|AWS_|S3_)" "$PROJECT_ROOT/.env" | grep -v '^#' | xargs)
elif [ -f "$PROJECT_ROOT/env.production" ]; then
    echo "[INFO] Carregando variáveis de $PROJECT_ROOT/env.production"
    export $(grep -E "^(POSTGRES_|DB_|AWS_|S3_)" "$PROJECT_ROOT/env.production" | grep -v '^#' | xargs)
fi

# ==========================================
# CONFIGURAÇÕES - AJUSTE CONFORME NECESSÁRIO
# ==========================================

# AWS S3
S3_BUCKET="${S3_BUCKET:-cpsystem-backups-dev}"
AWS_REGION="${AWS_REGION:-us-east-1}"

# PostgreSQL
DB_CONTAINER="${DB_CONTAINER:-cpsystem-db}"
DB_NAME="${DB_NAME:-${POSTGRES_DB:-cobranca}}"
DB_USER="${DB_USER:-${POSTGRES_USER:-gustavo}}"
DB_PASSWORD="${DB_PASSWORD:-${POSTGRES_PASSWORD:-139150}}"

# Backup settings
BACKUP_DIR="${BACKUP_DIR:-/tmp/cpsystem-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
LOCAL_RETENTION_DAYS="${LOCAL_RETENTION_DAYS:-3}"

# Logs
LOG_FILE="${LOG_FILE:-/var/log/cpsystem-backup.log}"

# ==========================================
# FUNÇÕES
# ==========================================

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

error() {
    log "ERROR: $1"
    exit 1
}

check_dependencies() {
    log "Verificando dependências..."
    
    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        error "Docker não está instalado"
    fi
    
    # Verificar AWS CLI
    if ! command -v aws &> /dev/null; then
        error "AWS CLI não está instalado. Instale com: sudo apt install awscli"
    fi
    
    # Verificar container PostgreSQL
    if ! docker ps | grep -q "$DB_CONTAINER"; then
        error "Container PostgreSQL '$DB_CONTAINER' não está rodando"
    fi
    
    log "Dependências verificadas ✓"
}

create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        mkdir -p "$BACKUP_DIR"
        log "Diretório de backup criado: $BACKUP_DIR"
    fi
}

perform_backup() {
    local timestamp=$(date +%Y-%m-%d_%H-%M-%S)
    local backup_file="cpsystem-backup-${timestamp}.sql.gz"
    local backup_path="${BACKUP_DIR}/${backup_file}"
    
    log "Iniciando backup do banco de dados..."
    log "Arquivo: $backup_file"
    
    # Fazer dump do PostgreSQL e comprimir
    if docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" | gzip > "$backup_path"; then
        local file_size=$(du -h "$backup_path" | cut -f1)
        log "Backup criado com sucesso! Tamanho: $file_size"
        CURRENT_BACKUP_PATH="$backup_path"
    else
        error "Falha ao criar backup do banco de dados"
    fi
}

upload_to_s3() {
    local backup_path="$1"
    if [ -z "$backup_path" ]; then return 1; fi
    local backup_file=$(basename "$backup_path")
    local s3_path="s3://${S3_BUCKET}/${backup_file}"
    
    log "Enviando backup para S3..."
    log "Destino: $s3_path"
    
    if aws s3 cp "$backup_path" "$s3_path" --region "$AWS_REGION"; then
        log "Upload para S3 concluído com sucesso ✓"
        return 0
    else
        error "Falha ao enviar backup para S3"
    fi
}

cleanup_local_backups() {
    log "Limpando backups locais antigos (>${LOCAL_RETENTION_DAYS} dias)..."

    local deleted_count=0

    # Encontrar e remover arquivos antigos
    while IFS= read -r file; do
        rm -f "$file"
        log "Removido backup local: $(basename "$file")"
        deleted_count=$((deleted_count + 1))
    done < <(find "$BACKUP_DIR" -name "cpsystem-backup-*.sql.gz" -type f -mtime +${LOCAL_RETENTION_DAYS})

    if [ $deleted_count -eq 0 ]; then
        log "Nenhum backup local antigo para remover"
    else
        log "Removidos $deleted_count backup(s) local(is) antigo(s)"
    fi
}

cleanup_s3_backups() {
    log "Limpando backups do S3 (>${RETENTION_DAYS} dias)..."

    local cutoff_date=$(date -d "${RETENTION_DAYS} days ago" +%Y-%m-%d)
    local deleted_count=0

    # Listar backups no S3 usando process substitution (evita subshell do pipe)
    while read -r line; do
        # Extrair nome do arquivo
        local filename=$(echo "$line" | awk '{print $4}')

        # Extrair data do nome do arquivo (formato: cpsystem-backup-YYYY-MM-DD_HH-MM-SS.sql.gz)
        local file_date=$(echo "$filename" | grep -oP '\d{4}-\d{2}-\d{2}' | head -1)

        if [ -n "$file_date" ] && [ "$file_date" \< "$cutoff_date" ]; then
            log "Removendo backup do S3: $filename"
            aws s3 rm "s3://${S3_BUCKET}/${filename}" --region "$AWS_REGION"
            deleted_count=$((deleted_count + 1))
        fi
    done < <(aws s3 ls "s3://${S3_BUCKET}/" --region "$AWS_REGION" | grep "cpsystem-backup-" || true)

    if [ $deleted_count -eq 0 ]; then
        log "Nenhum backup do S3 antigo para remover"
    else
        log "Removidos $deleted_count backup(s) do S3"
    fi
}

verify_backup() {
    local backup_path="$1"
    
    log "Verificando integridade do backup..."
    
    # Testar se o arquivo gzip é válido
    if gzip -t "$backup_path" 2>/dev/null; then
        log "Arquivo de backup válido ✓"
        return 0
    else
        error "Arquivo de backup está corrompido!"
    fi
}

show_summary() {
    local backup_path="$1"
    local backup_file=$(basename "$backup_path")
    
    log "==================== RESUMO ===================="
    log "Backup: $backup_file"
    log "Tamanho: $(du -h $backup_path | cut -f1)"
    log "S3 Bucket: s3://${S3_BUCKET}/"
    log "Retenção Local: ${LOCAL_RETENTION_DAYS} dias"
    log "Retenção S3: ${RETENTION_DAYS} dias"
    log "================================================"
}

# ==========================================
# MAIN
# ==========================================

main() {
    log "=========================================="
    log "Iniciando processo de backup"
    log "=========================================="
    
    # Verificações iniciais
    check_dependencies
    create_backup_dir
    
    # Fazer backup
    perform_backup
    backup_path="$CURRENT_BACKUP_PATH"
    
    # Verificar integridade
    verify_backup "$backup_path"
    
    # Upload para S3
    upload_to_s3 "$backup_path"
    
    # Limpeza
    cleanup_local_backups
    cleanup_s3_backups
    
    # Resumo
    show_summary "$backup_path"
    
    log "Backup concluído com sucesso! ✓"
    log "=========================================="
    
    exit 0
}

# Executar
main "$@"
