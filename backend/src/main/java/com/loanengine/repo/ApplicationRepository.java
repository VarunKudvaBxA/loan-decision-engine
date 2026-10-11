package com.loanengine.repo;

import com.loanengine.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findAllByOrderByIdDesc();
}
