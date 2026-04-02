package com.personal.book.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.book.api.dtos.BookDto;
import com.personal.book.api.dtos.CreateBookRequest;
import com.personal.book.api.dtos.IncreaseBookCopiesRequest;
import com.personal.book.api.dtos.UpdateAvailabilityRequest;
import com.personal.book.api.dtos.UpdateBookRequest;
import com.personal.book.api.mappers.BookMapper;
import com.personal.book.domain.entities.Book;
import com.personal.book.domain.exception.ErrorCode;
import com.personal.book.domain.exception.WebException;
import com.personal.book.domain.repositories.BookRepository;
import com.personal.book.events.BookLifecycleEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookLifecycleEventPublisher bookLifecycleEventPublisher;

    public BookService(BookRepository bookRepository,
            BookLifecycleEventPublisher bookLifecycleEventPublisher) {
        this.bookRepository = bookRepository;
        this.bookLifecycleEventPublisher = bookLifecycleEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<BookDto> getAllBook() {
        return bookRepository
                .findAll()
                .stream()
                .map(BookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookDto getBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));

        return BookMapper.toDto(book);
    }

    @Transactional
    public BookDto createNewBook(CreateBookRequest request) {

        Book book = BookMapper.toEntity(request);
        Book createdBook = bookRepository.save(book);

        log.info("Book created.");
        return BookMapper.toDto(createdBook);
    }

    @Transactional
    public BookDto updateAvailability(Long id, UpdateAvailabilityRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));
        book.setAvailableCopies(request.availableCopies());
        return BookMapper.toDto(bookRepository.save(book));
    }

    @Transactional
    public BookDto increaseStock(Long id, IncreaseBookCopiesRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));

        int additionalCopies = request.additionalCopies();
        book.setAvailableCopies(book.getAvailableCopies() + additionalCopies);
        book.setTotalCopies(book.getTotalCopies() + additionalCopies);
        return BookMapper.toDto(bookRepository.save(book));
    }

    @Transactional
    public BookDto updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));
        if (request.title() != null)
            book.setTitle(request.title());
        if (request.author() != null)
            book.setAuthor(request.author());
        if (request.category() != null)
            book.setCategory(request.category());
        if (request.isbn() != null)
            book.setIsbn(request.isbn());
        if (request.totalCopies() != null)
            book.setTotalCopies(request.totalCopies());
        return BookMapper.toDto(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));
        bookRepository.delete(book);
        bookLifecycleEventPublisher.publishDeleted(book);
        log.info("Book {} deleted.", id);
    }
}
