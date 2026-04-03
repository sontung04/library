package com.personal.loan.domain.services;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.personal.loan.api.dtos.ApiResponse;
import com.personal.loan.api.dtos.BookDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BookClient {

    private final RestClient restClient;

    public BookClient(@LoadBalanced RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://book-service")
                .build();
    }

    public BookDto getBook(Long bookId) {
        try {
            ApiResponse<BookDto> response = restClient.get()
                    .uri("/api/books/{id}", bookId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<BookDto>>() {
                    });
            if (response != null) {
                log.debug("Retrieved book {} with title: {}", bookId, response.data().title());
                return response.data();
            } else {
                log.warn("Empty response when fetching book {}", bookId);
                return null;
            }
        } catch (RestClientException e) {
            log.error("Failed to retrieve book {} from book-service", bookId, e);
            return null;
        }
    }

    public boolean updateAvailability(Long bookId, int newAvailableCopies) {
        try {
            restClient.patch()
                    .uri("/api/books/{id}/availability", bookId)
                    .body(new UpdateAvailabilityRequest(newAvailableCopies))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Updated availability for book {} to {}", bookId, newAvailableCopies);
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update availability for book {} in book-service", bookId, e);
            return false;
        }
    }

    private record UpdateAvailabilityRequest(int availableCopies) {
    }
}
