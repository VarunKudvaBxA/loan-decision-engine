package com.loanengine.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public final class Dtos {
    private Dtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record NoteRequest(String decision, String note) {
    }

    public record RuleUpdate(BigDecimal threshold, Boolean active) {
    }
}
