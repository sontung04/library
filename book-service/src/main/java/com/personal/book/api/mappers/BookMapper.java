package com.personal.book.api.mappers;

import com.personal.book.api.dtos.BookDto;
import com.personal.book.api.dtos.CreateBookRequest;
import com.personal.book.domain.entities.Book;

public class BookMapper {

    private BookMapper() {
    }

    public static BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }

        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getIsbn(),
                book.getAvailableCopies(),
                book.getTotalCopies());
    }

    public static Book toEntity(BookDto bookDto) {
        if (bookDto == null) {
            return null;
        }

        Book book = new Book();
        book.setTitle(bookDto.title());
        book.setAuthor(bookDto.author());
        book.setCategory(bookDto.category());
        book.setIsbn(bookDto.isbn());
        book.setAvailableCopies(bookDto.availableCopies());
        book.setTotalCopies(bookDto.totalCopies());
        return book;
    }

    public static Book toEntity(CreateBookRequest request) {

        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setCategory(request.category());
        book.setIsbn(request.isbn());
        book.setTotalCopies(request.copies().intValue());
        book.setAvailableCopies(request.copies().intValue());
        return book;
    }
}
