package com.personal.book.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.book.api.dtos.ApiResponse;
import com.personal.book.api.dtos.BookDto;
import com.personal.book.api.dtos.CreateBookRequest;
import com.personal.book.api.dtos.IncreaseBookCopiesRequest;
import com.personal.book.api.dtos.UpdateAvailabilityRequest;
import com.personal.book.api.dtos.UpdateBookRequest;
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
        return (bookList.isEmpty()) ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(new ApiResponse<>(bookList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> getBook(@PathVariable Long id) {
        log.info("Call GET /api/books/{} end point", id);
        BookDto book = bookService.getBook(id);
        return ResponseEntity.ok(new ApiResponse<>(FOUND_MESSAGE, book));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<BookDto>> createNewBook(@RequestBody CreateBookRequest request) {
        log.info("Call POST /api/books end point with LIBRARIAN authority, request: {} {} {} {} {}",
                request.title(),
                request.author(),
                request.category(),
                request.isbn(),
                request.copies().intValue());

        return ResponseEntity.ok(new ApiResponse<>(bookService.createNewBook(request)));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<BookDto>> updateAvailability(
            @PathVariable Long id,
            @RequestBody UpdateAvailabilityRequest request) {
        log.info("Call PATCH /api/books/{}/availability", id);
        return ResponseEntity.ok(new ApiResponse<>(bookService.updateAvailability(id, request)));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<BookDto>> increaseStock(
            @PathVariable Long id,
            @RequestBody IncreaseBookCopiesRequest request) {
        log.info("Call PATCH /api/books/{}/stock", id);
        return ResponseEntity.ok(new ApiResponse<>(bookService.increaseStock(id, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> updateBook(
            @PathVariable Long id,
            @RequestBody UpdateBookRequest request) {
        log.info("Call PUT /api/books/{}", id);
        return ResponseEntity.ok(new ApiResponse<>(bookService.updateBook(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        log.info("Call DELETE /api/books/{}", id);
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
