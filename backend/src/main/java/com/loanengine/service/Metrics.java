package com.loanengine.service;

import com.loanengine.dto.ApplicationRequest;

/** derived numbers that both the rules and the model read from an application */
public final class Metrics {
    private static final double MONTHLY_RATE = 0.01; // assume 12% APR for the new loan

    private Metrics() {
    }

    public static double payment(double principal, int months) {
        return principal * MONTHLY_RATE / (1 - Math.pow(1 + MONTHLY_RATE, -months));
    }

    public static double value(ApplicationRequest a, String field) {
        double income = a.annualIncome().doubleValue();
        double loan = a.loanAmount().doubleValue();
        switch (field) {
            case "dti":
                return (a.monthlyDebt().doubleValue() + payment(loan, a.termMonths())) / (income / 12.0) * 100.0;
            case "loan_to_income":
                return loan / income;
            case "credit_score":
                return a.creditScore();
            case "delinquencies":
                return a.delinquencies();
            case "employment_years":
                return a.employmentYears();
            case "term_months":
                return a.termMonths();
            case "age":
                return a.age();
            case "annual_income":
                return income;
            case "loan_amount":
                return loan;
            default:
                throw new IllegalArgumentException("unknown field " + field);
        }
    }
}
