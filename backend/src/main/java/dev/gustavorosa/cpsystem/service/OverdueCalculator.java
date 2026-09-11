package dev.gustavorosa.cpsystem.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Multa (fixa) + juros de mora (pro-rata diário, taxa mensal / 30) sobre um pagamento em atraso.
 * A soma sem arredondar ({@link #calculateSurcharge}) existe para que quem precisa somar vários
 * pagamentos (ex: relatórios) arredonde apenas o total final, em vez de somar centavos já
 * arredondados individualmente — o que diverge do valor combinado calculado pelo banco.
 */
public final class OverdueCalculator {

    private static final BigDecimal DAYS_IN_MONTH = BigDecimal.valueOf(30);

    private OverdueCalculator() {
    }

    public static BigDecimal calculateOverdueValue(BigDecimal originalValue, BigDecimal lateFeeRate,
            BigDecimal monthlyInterestRate, long daysOverdue) {
        return originalValue.add(calculateSurcharge(originalValue, lateFeeRate, monthlyInterestRate, daysOverdue))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateSurcharge(BigDecimal originalValue, BigDecimal lateFeeRate,
            BigDecimal monthlyInterestRate, long daysOverdue) {
        if (daysOverdue <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal effectiveLateFeeRate = lateFeeRate != null ? lateFeeRate : BigDecimal.ZERO;
        BigDecimal effectiveMonthlyInterestRate = monthlyInterestRate != null ? monthlyInterestRate : BigDecimal.ZERO;

        BigDecimal lateFee = originalValue.multiply(effectiveLateFeeRate);

        BigDecimal dailyInterestRate = effectiveMonthlyInterestRate
                .divide(DAYS_IN_MONTH, 10, RoundingMode.HALF_UP);
        BigDecimal totalInterest = originalValue.multiply(dailyInterestRate).multiply(BigDecimal.valueOf(daysOverdue));

        return lateFee.add(totalInterest);
    }
}
