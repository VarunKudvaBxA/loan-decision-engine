package com.loanengine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lending_rule")
@Getter
@Setter
@NoArgsConstructor
public class LendingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String field;
    private String operator;
    private BigDecimal threshold;
    private String outcome;
    private String message;
    private Boolean active;
    private Integer version;
}
