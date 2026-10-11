package com.loanengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanengine.dto.ApplicationRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

/**
 * Logistic regression that was trained offline (see /ml) and exported as json.
 * With standardised inputs, coef * z is each feature's exact contribution to the
 * log odds, so the reason codes need no extra library.
 */
@Component
public class RiskModel {

    public record Feature(String name, String label, double mean, double std, double coef) {
    }

    public record ModelSpec(String version, double intercept, List<Feature> features) {
    }

    public record Reason(String feature, String label, double value, double contribution, String direction) {
    }

    public record Score(double pd, List<Reason> reasons) {
    }

    private final ModelSpec spec;

    public RiskModel(ObjectMapper mapper) throws IOException {
        try (InputStream in = new ClassPathResource("model/model.json").getInputStream()) {
            this.spec = mapper.readValue(in, ModelSpec.class);
        }
    }

    public String version() {
        return spec.version();
    }

    public Score score(ApplicationRequest app) {
        double logit = spec.intercept();
        List<Reason> reasons = new java.util.ArrayList<>();
        for (Feature f : spec.features()) {
            double raw = Metrics.value(app, f.name());
            double contribution = f.coef() * (raw - f.mean()) / f.std();
            logit += contribution;
            String dir = contribution >= 0 ? "increases risk" : "reduces risk";
            reasons.add(new Reason(f.name(), f.label(), round(raw), round(contribution), dir));
        }
        double pd = 1.0 / (1.0 + Math.exp(-logit));
        reasons.sort(Comparator.comparingDouble((Reason r) -> Math.abs(r.contribution())).reversed());
        return new Score(Math.round(pd * 10000.0) / 10000.0, reasons.subList(0, Math.min(5, reasons.size())));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
