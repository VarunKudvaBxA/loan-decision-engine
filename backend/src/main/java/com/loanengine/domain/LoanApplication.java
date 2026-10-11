package com.loanengine.domain;

import com.loanengine.dto.ApplicationRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_application")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String DECLINED = "DECLINED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String applicantName;
    private Integer age;
    private BigDecimal annualIncome;
    private BigDecimal monthlyDebt;
    private BigDecimal loanAmount;
    private Integer termMonths;
    private Integer creditScore;
    private Integer delinquencies;
    private Double employmentYears;
    private String status;
    private String createdBy;
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public static LoanApplication from(ApplicationRequest r, String actor) {
        LoanApplication a = new LoanApplication();
        a.applicantName = (r.applicantName() == null || r.applicantName().isBlank()) ? "Unnamed applicant" : r.applicantName().trim();
        a.age = r.age();
        a.annualIncome = r.annualIncome();
        a.monthlyDebt = r.monthlyDebt();
        a.loanAmount = r.loanAmount();
        a.termMonths = r.termMonths();
        a.creditScore = r.creditScore();
        a.delinquencies = r.delinquencies();
        a.employmentYears = r.employmentYears();
        a.status = PENDING_REVIEW;
        a.createdBy = actor;
        return a;
    }

    public ApplicationRequest toRequest() {
        return new ApplicationRequest(applicantName, age, annualIncome, monthlyDebt, loanAmount,
                termMonths, creditScore, delinquencies, employmentYears);
    }
}
