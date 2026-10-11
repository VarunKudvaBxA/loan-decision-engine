package com.loanengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanengine.dto.ApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExplanationService {
    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);

    public record Explanation(String text, String source) {
    }

    private static final String SYSTEM = """
            You write short explanations of loan decisions for a bank reviewer.
            Use only the facts in the JSON the user sends. Do not invent numbers, reasons or policies.
            Write 3 or 4 plain sentences. No markdown, no bullet points, no headings.
            Start with the recommendation, then the main reasons, then anything the reviewer should double check.
            If a rule fired, name it. Mention the estimated default probability as a percentage.
            """;

    private final ObjectMapper mapper;
    private final RestClient http;
    private final String url;
    private final String apiKey;
    private final String model;

    public ExplanationService(ObjectMapper mapper,
                              @Value("${app.llm.url}") String url,
                              @Value("${app.llm.api-key}") String apiKey,
                              @Value("${app.llm.model}") String model) {
        this.mapper = mapper;
        this.url = url;
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public Explanation explain(ApplicationRequest app, EvaluationService.Evaluation ev) {
        if (apiKey == null || apiKey.isBlank()) {
            return new Explanation(template(ev), "TEMPLATE");
        }
        try {
            String facts = mapper.writeValueAsString(facts(app, ev));
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 400,
                    "system", SYSTEM,
                    "messages", List.of(Map.of("role", "user", "content", facts)));

            JsonNode res = http.post()
                    .uri(url)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = res == null ? "" : res.path("content").path(0).path("text").asText("").trim();
            if (text.isEmpty()) {
                return new Explanation(template(ev), "TEMPLATE");
            }
            return new Explanation(text, "LLM");
        } catch (Exception e) {
            log.warn("LLM call failed, using template instead: {}", e.getMessage());
            return new Explanation(template(ev), "TEMPLATE");
        }
    }

    private Map<String, Object> facts(ApplicationRequest app, EvaluationService.Evaluation ev) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recommendation", ev.recommendation());
        m.put("estimated_default_probability_percent", Math.round(ev.pd() * 1000.0) / 10.0);
        m.put("debt_to_income_percent", Math.round(Metrics.value(app, "dti") * 10.0) / 10.0);
        m.put("credit_score", app.creditScore());
        m.put("recent_delinquencies", app.delinquencies());
        m.put("loan_amount", app.loanAmount());
        m.put("annual_income", app.annualIncome());
        m.put("rules_triggered", ev.hits());
        m.put("top_model_factors", ev.reasons());
        return m;
    }

    private String template(EvaluationService.Evaluation ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recommendation: ").append(ev.recommendation()).append(". ");
        sb.append("Estimated default probability is ").append(Math.round(ev.pd() * 1000.0) / 10.0).append(" percent. ");
        if (ev.hits().isEmpty()) {
            sb.append("No policy rules were triggered. ");
        } else {
            sb.append("Rules triggered: ");
            for (int i = 0; i < ev.hits().size(); i++) {
                if (i > 0) sb.append("; ");
                sb.append(ev.hits().get(i).message());
            }
            sb.append(". ");
        }
        if (!ev.reasons().isEmpty()) {
            RiskModel.Reason top = ev.reasons().get(0);
            sb.append("The biggest model factor is ").append(top.label().toLowerCase())
                    .append(" (").append(top.value()).append("), which ").append(top.direction()).append(".");
        }
        return sb.toString();
    }
}
