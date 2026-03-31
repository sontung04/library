package com.personal.loan.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.personal.loan.domain.entities.LoanItem;

@Repository
public interface LoanItemRepository extends JpaRepository<LoanItem, Long> {
    
}
