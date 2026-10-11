package com.loanengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanengine.domain.AuditLog;
import com.loanengine.domain.Decision;
import com.loanengine.domain.LoanApplication;
import com.loanengine.dto.ApplicationRequest;
import com.loanengine.repo.ApplicationRepository;
import com.loanengine.repo.DecisionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class DecisionService {

    public record Detail(LoanApplication application, Map<String, Object> decision, List<AuditLog> audit) {
    }

    private final ApplicationRepository apps;
    private final DecisionRepository decisions;
    private final EvaluationService evaluator;
    private final ExplanationService explainer;
    private final AuditService audit;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    public DecisionService(ApplicationRepository apps, DecisionRepository decisions, EvaluationService evaluator,
                           ExplanationService explainer, AuditService audit, ObjectMapper mapper,
                           PlatformTransactionManager txManager) {
        this.apps = apps;
        this.decisions = decisions;
        this.evaluator = evaluator;
        this.explainer = explainer;
        this.audit = audit;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(txManager);
    }

    /** scores the application and asks the LLM for the write-up before opening a db transaction */
    public LoanApplication create(ApplicationRequest req, String actor) {
        EvaluationService.Evaluation ev = evaluator.evaluate(req);
        ExplanationService.Explanation ex = explainer.explain(req, ev);

        return tx.execute(status -> {
            LoanApplication app = apps.save(LoanApplication.from(req, actor));

            Decision d = new Decision();
            d.setApplicationId(app.getId());
            d.setSystemRecommendation(ev.recommendation());
            d.setRiskScore(ev.pd());
            d.setModelVersion(ev.modelVersion());
            d.setRulesVersion(ev.rulesVersion());
            d.setRuleHits(json(ev.hits()));
            d.setReasonCodes(json(ev.reasons()));
            d.setExplanation(ex.text());
            d.setExplanationSource(ex.source());
            decisions.save(d);

            audit.log("APPLICATION", app.getId(), "SUBMITTED", actor, "applicant " + app.getApplicantName());
            audit.log("APPLICATION", app.getId(), "SCORED", "system",
                    "recommendation " + ev.recommendation() + ", pd " + ev.pd()
                            + ", " + ev.modelVersion() + ", " + ev.rulesVersion());
            return app;
        });
    }

    @Transactional
    public void recommend(Long appId, String recommendation, String note, String actor) {
        if (!Set.of("APPROVE", "DECLINE", "REFER").contains(recommendation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recommendation must be APPROVE, DECLINE or REFER");
        }
        LoanApplication app = find(appId);
        if (!LoanApplication.PENDING_REVIEW.equals(app.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application is not waiting for an analyst review");
        }
        Decision d = decisionFor(appId);
        d.setAnalystRecommendation(recommendation);
        d.setAnalystNote(note);
        d.setRecommendedBy(actor);
        d.setRecommendedAt(Instant.now());
        app.setStatus(LoanApplication.PENDING_APPROVAL);

        String overridden = recommendation.equals(d.getSystemRecommendation()) ? "" : " (overrides system: " + d.getSystemRecommendation() + ")";
        audit.log("APPLICATION", appId, "RECOMMENDED", actor, recommendation + overridden);
    }

    @Transactional
    public void finalizeDecision(Long appId, String decision, String note, String actor) {
        if (!Set.of("APPROVE", "DECLINE").contains(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Final decision must be APPROVE or DECLINE");
        }
        LoanApplication app = find(appId);
        if (!LoanApplication.PENDING_APPROVAL.equals(app.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application is not waiting for approval");
        }
        Decision d = decisionFor(appId);
        if (actor.equals(d.getRecommendedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The person who recommended cannot also approve");
        }
        d.setFinalDecision(decision);
        d.setFinalNote(note);
        d.setDecidedBy(actor);
        d.setDecidedAt(Instant.now());
        app.setStatus("APPROVE".equals(decision) ? LoanApplication.APPROVED : LoanApplication.DECLINED);

        audit.log("APPLICATION", appId, "FINALIZED", actor, decision);
    }

    public Detail detail(Long appId) {
        LoanApplication app = find(appId);
        Decision d = decisionFor(appId);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", d.getId());
        view.put("systemRecommendation", d.getSystemRecommendation());
        view.put("riskScore", d.getRiskScore());
        view.put("modelVersion", d.getModelVersion());
        view.put("rulesVersion", d.getRulesVersion());
        view.put("ruleHits", parse(d.getRuleHits()));
        view.put("reasonCodes", parse(d.getReasonCodes()));
        view.put("explanation", d.getExplanation());
        view.put("explanationSource", d.getExplanationSource());
        view.put("analystRecommendation", d.getAnalystRecommendation());
        view.put("analystNote", d.getAnalystNote());
        view.put("recommendedBy", d.getRecommendedBy());
        view.put("finalDecision", d.getFinalDecision());
        view.put("finalNote", d.getFinalNote());
        view.put("decidedBy", d.getDecidedBy());

        return new Detail(app, view, audit.forEntity("APPLICATION", appId));
    }

    public List<Map<String, Object>> list() {
        Map<Long, Decision> byApp = new HashMap<>();
        for (Decision d : decisions.findAll()) {
            byApp.put(d.getApplicationId(), d);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LoanApplication a : apps.findAllByOrderByIdDesc()) {
            Decision d = byApp.get(a.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("applicantName", a.getApplicantName());
            row.put("loanAmount", a.getLoanAmount());
            row.put("status", a.getStatus());
            row.put("createdAt", a.getCreatedAt());
            row.put("riskScore", d == null ? null : d.getRiskScore());
            row.put("systemRecommendation", d == null ? null : d.getSystemRecommendation());
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> stats() {
        Map<String, Integer> byStatus = new TreeMap<>();
        Map<String, Integer> byRecommendation = new TreeMap<>();
        int[] buckets = new int[10];
        double total = 0;
        int n = 0;
        for (LoanApplication a : apps.findAll()) {
            byStatus.merge(a.getStatus(), 1, Integer::sum);
        }
        for (Decision d : decisions.findAll()) {
            byRecommendation.merge(d.getSystemRecommendation(), 1, Integer::sum);
            buckets[Math.min(9, (int) (d.getRiskScore() * 10))]++;
            total += d.getRiskScore();
            n++;
        }
        List<Map<String, Object>> histogram = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            histogram.add(Map.of("range", (i * 10) + "-" + ((i + 1) * 10) + "%", "count", buckets[i]));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", n);
        out.put("averageRisk", n == 0 ? 0 : Math.round(total / n * 1000.0) / 1000.0);
        out.put("byStatus", byStatus);
        out.put("byRecommendation", byRecommendation);
        out.put("riskHistogram", histogram);
        return out;
    }

    private LoanApplication find(Long id) {
        return apps.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private Decision decisionFor(Long appId) {
        return decisions.findByApplicationId(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Decision not found"));
    }

    private String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object parse(String s) {
        try {
            return mapper.readTree(s);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
