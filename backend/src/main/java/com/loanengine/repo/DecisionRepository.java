package com.loanengine.repo;

import com.loanengine.domain.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
    Optional<Decision> findByApplicationId(Long applicationId);
}
