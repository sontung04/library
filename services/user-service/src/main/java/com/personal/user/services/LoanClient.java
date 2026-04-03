package com.personal.user.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoanClient {

    private final RestClient restClient;

    @Value("${app.internal.api-key:library-internal-key}")
    private String internalApiKey;

    public LoanClient(@LoadBalanced RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://loan-service")
                .build();
    }

    public boolean hasActiveLoans(Long userId) {
        try {
            ActiveLoanCheckResponse response = restClient.get()
                    .uri("/internal/loans/users/{userId}/active-exists", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ActiveLoanCheckResponse>() {
                    });
            boolean hasActive = response != null && response.hasActiveLoans();
            log.debug("User {} has active loans: {}", userId, hasActive);
            return hasActive;
        } catch (RestClientException e) {
            log.error("Failed to check active loans for user {} from loan-service", userId, e);
            return false;
        }
    }

    private record ActiveLoanCheckResponse(boolean hasActiveLoans) {
    }
}
