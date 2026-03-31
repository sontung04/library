package com.personal.book.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.personal.book.domain.entities.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
}
