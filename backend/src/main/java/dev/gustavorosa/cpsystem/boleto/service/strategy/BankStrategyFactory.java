package dev.gustavorosa.cpsystem.boleto.service.strategy;

import dev.gustavorosa.cpsystem.boleto.model.BankType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BankStrategyFactory {
    private final Map<BankType, BankBoletoStrategy> strategies = new EnumMap<>(BankType.class);
    
    public BankStrategyFactory(List<BankBoletoStrategy> strategyList) {
        strategyList.forEach(strategy -> 
            strategies.put(strategy.getSupportedBank(), strategy)
        );
    }
    
    public BankBoletoStrategy getStrategy(BankType bankType) {
        BankBoletoStrategy strategy = strategies.get(bankType);
        if (strategy == null) {
            throw new IllegalArgumentException("Banco não suportado: " + bankType);
        }
        return strategy;
    }
}
