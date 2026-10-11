package com.loanengine.service;

import com.loanengine.domain.AuditLog;
import com.loanengine.repo.AuditLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

/**
 * Each row stores the hash of the previous row, so editing history after the fact
 * breaks the chain. The table also blocks update and delete with a trigger.
 */
@Service
public class AuditService {

    public record Verification(boolean valid, int checked, Long brokenAtId) {
    }

    private final AuditLogRepository repo;
    private final JdbcTemplate jdbc;

    public AuditService(AuditLogRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @Transactional
    public AuditLog log(String entityType, Long entityId, String action, String actor, String details) {
        // one writer at a time so two requests can't both extend the same previous row
        jdbc.execute("select pg_advisory_xact_lock(7001)");

        String prev = repo.findTopByOrderByIdDesc().map(AuditLog::getHash).orElse("GENESIS");
        AuditLog row = new AuditLog();
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        row.setAction(action);
        row.setActor(actor);
        row.setDetails(details);
        row.setPrevHash(prev);
        row.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        row.setHash(hash(row));
        return repo.save(row);
    }

    public List<AuditLog> forEntity(String entityType, Long entityId) {
        return repo.findByEntityTypeAndEntityIdOrderByIdAsc(entityType, entityId);
    }

    public Verification verify() {
        String expectedPrev = "GENESIS";
        int count = 0;
        for (AuditLog row : repo.findAllByOrderByIdAsc()) {
            if (!expectedPrev.equals(row.getPrevHash()) || !hash(row).equals(row.getHash())) {
                return new Verification(false, count, row.getId());
            }
            expectedPrev = row.getHash();
            count++;
        }
        return new Verification(true, count, null);
    }

    private String hash(AuditLog r) {
        String raw = String.join("|", r.getPrevHash(), r.getEntityType(), String.valueOf(r.getEntityId()),
                r.getAction(), r.getActor(), String.valueOf(r.getDetails()), r.getCreatedAt().toString());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
