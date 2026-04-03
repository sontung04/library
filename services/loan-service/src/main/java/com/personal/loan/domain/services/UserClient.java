package com.personal.loan.domain.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserClient {

    private final RestClient restClient;

    @Value("${app.internal.api-key:library-internal-key}")
    private String internalApiKey;

    public UserClient(@LoadBalanced RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://user-service")
                .build();
    }

    public String getUsername(Long userId) {
        try {
            InternalUserAuthSnapshotResponse response = restClient.get()
                    .uri("/internal/auth/users/{id}", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<InternalUserAuthSnapshotResponse>() {
                    });
            if (response != null) {
                log.debug("Retrieved username for user {}: {}", userId, response.username());
                return response.username();
            } else {
                log.warn("Empty response when fetching username for user {}", userId);
                return String.valueOf(userId);
            }
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            switch (status) {
                case 401, 403:
                    log.warn("Internal auth call to user-service rejected for userId={} with status {}. Check INTERNAL_API_KEY consistency.",
                            userId, status);
                    break;
                case 404:
                    log.warn("User {} not found in user-service while creating loan snapshot.", userId);
                    break;
                default:
                    log.error("Failed to retrieve username for user {} from user-service, status={}", userId, status, e);
            }
            return String.valueOf(userId);
        } catch (RestClientException e) {
            log.error("Failed to retrieve username for user {} from user-service", userId, e);
            return String.valueOf(userId);
        }
    }

    private record InternalUserAuthSnapshotResponse(Long userId, String username, List<String> roles) {
    }
}
