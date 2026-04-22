package com.personal.book.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.book.api.dtos.BookDto;
import com.personal.book.api.dtos.CreateBookRequest;
import com.personal.book.api.dtos.IncreaseBookCopiesRequest;
import com.personal.book.api.dtos.UpdateBookRequest;
import com.personal.book.api.mappers.BookMapper;
import com.personal.book.domain.entities.Book;
import com.personal.book.domain.exception.ErrorCode;
import com.personal.book.domain.exception.WebException;
import com.personal.book.domain.repositories.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.book.events.BookLifecycleEvent;
import com.personal.book.events.LifecycleAction;
import com.personal.book.outbox.KafkaOutboxEvent;
import com.personal.book.outbox.KafkaOutboxEventRepository;
import com.personal.book.api.dtos.KafkaBookEventPayload;
import com.personal.book.idempotency.ProcessedEvent;
import com.personal.book.idempotency.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookService implements BookAvailabilityEventHandler {

    private static final String BOOK_LIFECYCLE_TOPIC = "book-lifecycle";

    private final BookRepository bookRepository;
    private final KafkaOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    public BookService(BookRepository bookRepository,
            KafkaOutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository) {
        this.bookRepository = bookRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
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

        saveOutboxEvent(BookMapper.toKafkaPayload(createdBook), LifecycleAction.CREATE);
        log.info("Book created with id = {}.", createdBook.getId());
        return BookMapper.toDto(createdBook);
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
        Book savedBook = bookRepository.save(book);
        saveOutboxEvent(BookMapper.toKafkaPayload(savedBook), LifecycleAction.UPDATE);
        return BookMapper.toDto(savedBook);
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

        Book savedBook = bookRepository.save(book);
        saveOutboxEvent(BookMapper.toKafkaPayload(savedBook), LifecycleAction.UPDATE);
        return BookMapper.toDto(savedBook);
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

        saveOutboxEvent(BookMapper.toKafkaPayload(book), LifecycleAction.DELETE);
        bookRepository.delete(book);

        log.info("Book {} deleted.", id);
    }

    private void saveOutboxEvent(KafkaBookEventPayload payload, LifecycleAction action) {
        try {
            String message = objectMapper.writeValueAsString(new BookLifecycleEvent(payload, action));
            outboxRepository.save(new KafkaOutboxEvent(BOOK_LIFECYCLE_TOPIC, String.valueOf(payload.id()), message));
        } catch (Exception e) {
            log.error("Failed to serialize outbox event for bookId={}, action={}", payload.id(), action, e);
            throw new WebException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    @Transactional
    public void handleLoanEvent(String eventId, Long bookId) {

        if (processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate LOAN event id={} for bookId={}, skipping.", eventId, bookId);
            return;
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));

        int availableCopies = book.getAvailableCopies();
        if (availableCopies == 0)
            throw new WebException(ErrorCode.OUT_OF_STOCK);

        book.setAvailableCopies(availableCopies - 1);
        int newAvailableCopies = bookRepository.save(book).getAvailableCopies();
        processedEventRepository.save(new ProcessedEvent(eventId, java.time.LocalDateTime.now()));

        log.info("Handled book loan event id={}, bookId={}, copies {} → {}.",
                eventId, bookId, availableCopies, newAvailableCopies);
    }

    @Override
    @Transactional
    public void handleReturnEvent(String eventId, Long bookId) {

        if (processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate RETURN event id={} for bookId={}, skipping.", eventId, bookId);
            return;
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new WebException(ErrorCode.BOOK_NOT_FOUND));

        int availableCopies = book.getAvailableCopies();
        book.setAvailableCopies(availableCopies + 1);
        int newAvailableCopies = bookRepository.save(book).getAvailableCopies();
        processedEventRepository.save(new ProcessedEvent(eventId, java.time.LocalDateTime.now()));

        log.info("Handled book return event id={}, bookId={}, copies {} → {}.",
                eventId, bookId, availableCopies, newAvailableCopies);
    }
}
