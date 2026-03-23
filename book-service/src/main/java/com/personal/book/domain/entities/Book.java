package com.personal.book.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
public class Book {
    
    @Id
    private Long id;

    @NotNull
    private String title;

    private String author;

    private String category;

    @NotNull
    private String isbn;

    @NotNull
    private int availableCopies;

    @NotNull
    private int totalCopies;
}
