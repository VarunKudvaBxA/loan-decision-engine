package com.loanengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanengine.dto.ApplicationRequest;
import com.loanengine.service.RiskModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskModelTest {

    private final RiskModel model = load();

    private static RiskModel load() {
        try {
            return new RiskModel(new ObjectMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ApplicationRequest app(int score, int delinquencies, String monthlyDebt) {
        return new ApplicationRequest("Test", 35, new BigDecimal("60000"), new BigDecimal(monthlyDebt),
                new BigDecimal("15000"), 48, score, delinquencies, 6.0);
    }

    @Test
    void riskierProfileGetsHigherDefaultProbability() {
        double safe = model.score(app(780, 0, "300")).pd();
        double risky = model.score(app(560, 3, "2200")).pd();
        assertTrue(risky > safe, "risky " + risky + " should be above safe " + safe);
    }

    @Test
    void probabilityStaysBetweenZeroAndOne() {
        double pd = model.score(app(300, 10, "9000")).pd();
        assertTrue(pd > 0 && pd <= 1);
    }

    @Test
    void returnsAtMostFiveReasons() {
        assertEquals(5, model.score(app(700, 1, "800")).reasons().size());
    }
}
