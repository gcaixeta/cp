package dev.gustavorosa.cpsystem.api.response;

public record RecalculateOverdueInterestResponse(
        int paymentsMarkedOverdue,
        int paymentsRecalculated
) {
}
