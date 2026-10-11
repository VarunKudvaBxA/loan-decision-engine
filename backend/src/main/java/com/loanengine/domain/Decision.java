package com.loanengine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "decision")
@Getter
@Setter
@NoArgsConstructor
public class Decision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long applicationId;
    private String systemRecommendation;
    private Double riskScore;
    private String modelVersion;
    private String rulesVersion;
    private String ruleHits;
    private String reasonCodes;
    private String explanation;
    private String explanationSource;
    private String analystRecommendation;
    private String analystNote;
    private String recommendedBy;
    private Instant recommendedAt;
    private String finalDecision;
    private String finalNote;
    private String decidedBy;
    private Instant decidedAt;
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
