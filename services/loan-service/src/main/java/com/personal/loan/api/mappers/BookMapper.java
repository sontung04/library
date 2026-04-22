package com.personal.loan.api.mappers;

import com.personal.loan.api.dtos.BookDto;
import com.personal.loan.api.dtos.KafkaBookEventPayload;
import com.personal.loan.domain.entities.Book;

public class BookMapper {

    private BookMapper() {
    }

    public static Book toEntity(KafkaBookEventPayload payload) {
        return new Book(
                payload.id(),
                payload.title(),
                payload.isbn(),
                payload.availableCopies());
    }

    public static BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getAvailableCopies());
    }

    public static Book toEntity(BookDto bookDto) {
        return new Book(bookDto.id(), bookDto.title(), bookDto.isbn(), bookDto.availableCopies());
    }
}
