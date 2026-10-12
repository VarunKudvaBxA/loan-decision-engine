package com.loanengine.web;

import com.loanengine.domain.LoanApplication;
import com.loanengine.dto.ApplicationRequest;
import com.loanengine.dto.Dtos.NoteRequest;
import com.loanengine.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final DecisionService service;

    public ApplicationController(DecisionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public DecisionService.Detail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ANALYST','SENIOR')")
    public Map<String, Object> create(@Valid @RequestBody ApplicationRequest req, Authentication auth) {
        LoanApplication app = service.create(req, auth.getName());
        return Map.of("id", app.getId());
    }

    @PostMapping("/{id}/recommend")
    @PreAuthorize("hasRole('ANALYST')")
    public Map<String, String> recommend(@PathVariable Long id, @RequestBody NoteRequest body, Authentication auth) {
        service.recommend(id, body.decision(), body.note(), auth.getName());
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('SENIOR')")
    public Map<String, String> finalizeDecision(@PathVariable Long id, @RequestBody NoteRequest body, Authentication auth) {
        service.finalizeDecision(id, body.decision(), body.note(), auth.getName());
        return Map.of("status", "ok");
    }
}
