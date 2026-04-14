package com.personal.loan.domain.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.loan.api.dtos.KafkaBookEventPayload;
import com.personal.loan.api.mappers.BookMapper;
import com.personal.loan.domain.entities.Book;
import com.personal.loan.domain.repositories.BookRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService implements LifecycleEventHandler<Long, KafkaBookEventPayload> {

    private final LoanService loanService;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void handleCreationEvent(KafkaBookEventPayload payload) {

        Action bookAction = Action.CREATE;
        Book book = BookMapper.toEntity(payload);

        Long savedBookId = bookRepository.save(book).getId();
        logAction(savedBookId, bookAction);
    }

    @Override
    @Transactional
    public void handleUpdateEvent(KafkaBookEventPayload payload) {

        Optional<Book> book = bookRepository.findById(payload.id());
        Book bookEntity;
        Action bookAction = Action.UPDATE;

        // Create a new book if book is not found in db
        if (book.isEmpty()) {
            log.info("Book with id = {} cannot be found in database. Creating a new book.", payload.id());
            bookAction = Action.CREATE;
            bookEntity = BookMapper.toEntity(payload);
        } else {
            bookEntity = book.get();
            bookEntity.setIsbn(payload.isbn());
            bookEntity.setTitle(payload.title());
            bookEntity.setAvailableCopies(payload.availableCopies());
        }

        Long savedBookId = bookRepository.save(bookEntity).getId();
        logAction(savedBookId, bookAction);
    }

    @Override
    @Transactional
    public void handleDeletionEvent(Long id) {

        Action bookAction = Action.DELETE;

        loanService.deleteBookId(id);
        bookRepository.deleteById(id);
        logAction(id, bookAction);
    }

    private void logAction(Long bookId, Action bookAction) {
        log.info("Book {} with id = {}", bookAction.label(), bookId);
    }
}
