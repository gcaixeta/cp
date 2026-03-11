CREATE TABLE boletos (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    bank_type VARCHAR(50) NOT NULL,
    bank_boleto_id VARCHAR(100) UNIQUE,
    barcode VARCHAR(54),
    digitable_line VARCHAR(54),
    pdf_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    bank_api_response TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_boleto_payment FOREIGN KEY (payment_id) 
        REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX idx_boletos_payment_id ON boletos(payment_id);
CREATE INDEX idx_boletos_status ON boletos(status);
CREATE INDEX idx_boletos_bank_boleto_id ON boletos(bank_boleto_id);
