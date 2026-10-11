package com.loanengine.repo;

import com.loanengine.domain.LendingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<LendingRule, Long> {
    List<LendingRule> findByActiveTrue();
    List<LendingRule> findAllByOrderByIdAsc();
}
