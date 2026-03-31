package com.personal.loan.domain.services;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.personal.loan.api.dtos.ApiResponse;
import com.personal.loan.api.dtos.BookDto;

@Component
public class BookClient {

    private final RestClient restClient;

    public BookClient(@LoadBalanced RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://book-service")
                .build();
    }

    public BookDto getBook(Long bookId) {
        ApiResponse<BookDto> response = restClient.get()
                .uri("/api/books/{id}", bookId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<BookDto>>() {
                });
        return response != null ? response.data() : null;
    }

    public void updateAvailability(Long bookId, int newAvailableCopies) {
        restClient.patch()
                .uri("/api/books/{id}/availability", bookId)
                .body(new UpdateAvailabilityRequest(newAvailableCopies))
                .retrieve()
                .toBodilessEntity();
    }

    private record UpdateAvailabilityRequest(int availableCopies) {
    }
}
