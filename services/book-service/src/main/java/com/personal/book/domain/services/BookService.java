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
import com.personal.book.events.Action;
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

    /**
     * Returns all books in the catalogue.
     *
     * @return a list of {@link BookDto} for every book; empty if the catalogue is
     *         empty
     */
    @Transactional(readOnly = true)
    public List<BookDto> getAllBook() {
        return bookRepository
                .findAll()
                .stream()
                .map(BookMapper::toDto)
                .toList();
    }

    /**
     * Returns a single book by its ID.
     *
     * @param id the ID of the book to retrieve
     * @return the matching {@link BookDto}
     * @throws WebException with {@link ErrorCode#BOOK_NOT_FOUND} if no book exists
     *                      for the given ID
     */
    @Transactional(readOnly = true)
    public BookDto getBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));

        return BookMapper.toDto(book);
    }

    /**
     * Adds a new book to the catalogue.
     *
     * @param request the creation payload containing title, author, ISBN, category,
     *                and copy counts
     * @return a {@link BookDto} representing the newly persisted book
     */
    @Transactional
    public BookDto createNewBook(CreateBookRequest request) {

        Book book = BookMapper.toEntity(request);
        Book createdBook = bookRepository.save(book);

        bookLifecycleEventPublisher.publish(BookMapper.toKafkaPayload(createdBook), Action.CREATE);
        log.info("Book created with id = {}.", createdBook.getId());
        return BookMapper.toDto(createdBook);
    }

    /**
     * Directly sets the number of available copies for a book.
     * Used internally by loan-service to decrement or restore stock without
     * fetching the full entity.
     *
     * @param id      the ID of the book to update
     * @param request the payload containing the new {@code availableCopies} value
     * @return the updated {@link BookDto}
     * @throws WebException with {@link ErrorCode#BOOK_NOT_FOUND} if the book does
     *                      not exist
     */
    @Transactional
    public BookDto updateAvailability(Long id, UpdateAvailabilityRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));
        book.setAvailableCopies(request.availableCopies());
        return BookMapper.toDto(bookRepository.save(book));
    }

    /**
     * Increases both the total and available copy counts of a book by a given
     * amount.
     * Use this when new physical copies arrive; it adjusts both counters
     * atomically.
     *
     * @param id      the ID of the book to restock
     * @param request the payload containing {@code additionalCopies} to add
     * @return the updated {@link BookDto} reflecting the new counts
     * @throws WebException with {@link ErrorCode#BOOK_NOT_FOUND} if the book does
     *                      not exist
     */
    @Transactional
    public BookDto increaseStock(Long id, IncreaseBookCopiesRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));

        int additionalCopies = request.additionalCopies();
        book.setAvailableCopies(book.getAvailableCopies() + additionalCopies);
        book.setTotalCopies(book.getTotalCopies() + additionalCopies);
        return BookMapper.toDto(bookRepository.save(book));
    }

    /**
     * Partially updates a book's metadata. Only non-{@code null} fields in the
     * request are applied;
     * omitted fields retain their current values.
     *
     * @param id      the ID of the book to update
     * @param request the update payload (title, author, category, ISBN, totalCopies
     *                — all optional)
     * @return the updated {@link BookDto}
     * @throws WebException with {@link ErrorCode#BOOK_NOT_FOUND} if the book does
     *                      not exist
     */
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

        bookLifecycleEventPublisher.publish(
                BookMapper.toKafkaPayload(book), 
                Action.DELETE);
        return BookMapper.toDto(bookRepository.save(book));
    }

    /**
     * Permanently removes a book from the catalogue and publishes a
     * {@code BookDeletedEvent}
     * to Kafka so that loan-service can mark any historical loan records
     * accordingly.
     *
     * @param id the ID of the book to delete
     * @throws WebException with {@link ErrorCode#BOOK_NOT_FOUND} if the book does
     *                      not exist
     */
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));
        bookRepository.delete(book);
        bookLifecycleEventPublisher.publish(
                BookMapper.toKafkaPayload(book), 
                Action.DELETE);
        log.info("Book {} deleted.", id);
    }
}
