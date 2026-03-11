package dev.gustavorosa.cpsystem.boleto.model;

public enum BankType {
    INTER("077"),
    ITAU("341"),
    BRADESCO("237"),
    BANCO_DO_BRASIL("001");
    
    private final String code;
    
    BankType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
