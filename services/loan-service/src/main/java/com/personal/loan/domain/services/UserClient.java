package com.personal.loan.domain.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        InternalUserAuthSnapshotResponse response = restClient.get()
                .uri("/internal/auth/users/{id}", userId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<InternalUserAuthSnapshotResponse>() {
                });
        return response != null ? response.username() : String.valueOf(userId);
    }

    private record InternalUserAuthSnapshotResponse(Long userId, String username, List<String> roles) {
    }
}
