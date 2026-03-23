package com.personal.book.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.book.api.dtos.ApiResponse;
import com.personal.book.api.dtos.BookDto;
import com.personal.book.api.dtos.CreateBookRequest;
import com.personal.book.domain.services.BookService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    private static final String FOUND_MESSAGE = "Found";

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookDto>>> getAllBooks() {
        log.info("Call GET /api/books end point");
        List<BookDto> bookList = bookService.getAllBook();
        return (bookList.isEmpty()) ? 
                ResponseEntity.noContent().build() :
                ResponseEntity.ok(new ApiResponse<>(bookList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> getBook(@PathVariable Long id) {
        log.info("Call GET /api/books/{} end point", id);
        BookDto book = bookService.getBook(id);
        return (book == null) ?
                ResponseEntity.noContent().build() :
                ResponseEntity.ok(new ApiResponse<>(FOUND_MESSAGE, book));
    }

    @PostMapping()
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookDto>> createNewBook(@RequestBody CreateBookRequest request) {
        log.info("Call POST /api/books end point with LIBRARIAN authority");
        return ResponseEntity.ok(new ApiResponse<>(bookService.createNewBook(request)));
    }
}
