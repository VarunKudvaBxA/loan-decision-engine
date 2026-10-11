package com.loanengine.service;

import com.loanengine.dto.ApplicationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvaluationService {

    public record Evaluation(List<RuleEngine.Hit> hits, double pd, List<RiskModel.Reason> reasons,
                             String recommendation, String modelVersion, String rulesVersion) {
    }

    private final RuleEngine rules;
    private final RiskModel model;
    private final double declinePd;
    private final double referPd;

    public EvaluationService(RuleEngine rules, RiskModel model,
                             @Value("${app.risk.decline-pd}") double declinePd,
                             @Value("${app.risk.refer-pd}") double referPd) {
        this.rules = rules;
        this.model = model;
        this.declinePd = declinePd;
        this.referPd = referPd;
    }

    public Evaluation evaluate(ApplicationRequest app) {
        List<RuleEngine.Hit> hits = rules.evaluate(app);
        RiskModel.Score score = model.score(app);

        boolean hardDecline = hits.stream().anyMatch(h -> "DECLINE".equals(h.outcome()));
        boolean softRefer = hits.stream().anyMatch(h -> "REFER".equals(h.outcome()));

        String rec;
        if (hardDecline || score.pd() >= declinePd) {
            rec = "DECLINE";
        } else if (softRefer || score.pd() >= referPd) {
            rec = "REFER";
        } else {
            rec = "APPROVE";
        }
        return new Evaluation(hits, score.pd(), score.reasons(), rec, model.version(), rules.rulesVersion());
    }
}
