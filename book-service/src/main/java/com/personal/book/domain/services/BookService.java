package com.personal.book.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.personal.book.api.dtos.BookDto;
import com.personal.book.api.dtos.CreateBookRequest;
import com.personal.book.api.mappers.BookMapper;
import com.personal.book.domain.entities.Book;
import com.personal.book.domain.repositories.BookRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookDto> getAllBook() {
        return bookRepository
                .findAll()
                .stream()
                .map(BookMapper::toDto)
                .toList();
    }

    public BookDto getBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElse(null);

        return BookMapper.toDto(book);
    }

    public BookDto createNewBook(CreateBookRequest request) {
        
        Book book = BookMapper.toEntity(request);
        Book createdBook = bookRepository.save(book);
        
        log.info("Book created.");
        return BookMapper.toDto(createdBook);
    }
}
