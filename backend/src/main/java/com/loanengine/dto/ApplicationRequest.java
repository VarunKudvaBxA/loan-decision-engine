package com.loanengine.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ApplicationRequest(
        String applicantName,
        @NotNull @Min(0) @Max(120) Integer age,
        @NotNull @DecimalMin("1") BigDecimal annualIncome,
        @NotNull @DecimalMin("0") BigDecimal monthlyDebt,
        @NotNull @DecimalMin("1") BigDecimal loanAmount,
        @NotNull @Min(3) @Max(360) Integer termMonths,
        @NotNull @Min(300) @Max(850) Integer creditScore,
        @NotNull @Min(0) Integer delinquencies,
        @NotNull @DecimalMin("0") Double employmentYears
) {
}
