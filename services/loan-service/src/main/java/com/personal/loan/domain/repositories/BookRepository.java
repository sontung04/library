package com.personal.loan.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.loan.domain.entities.Book;

public interface BookRepository extends JpaRepository<Book, Long>{
    
}
