package com.personal.loan.domain.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanStatus;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);

    List<Loan> findByBookId(Long bookId);

    boolean existsByUserIdAndStatus(Long userId, LoanStatus status);
}
