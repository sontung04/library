package com.personal.user.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        ActiveLoanCheckResponse response = restClient.get()
                .uri("/internal/loans/users/{userId}/active-exists", userId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<ActiveLoanCheckResponse>() {
                });
        return response != null && response.hasActiveLoans();
    }

    private record ActiveLoanCheckResponse(boolean hasActiveLoans) {
    }
}
