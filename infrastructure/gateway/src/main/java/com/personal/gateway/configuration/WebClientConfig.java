package com.personal.gateway.configuration;

import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient webClient(ReactorLoadBalancerExchangeFilterFunction loadBalancerFilterFunction) {
        return WebClient.builder()
                .filter(loadBalancerFilterFunction)
                .build();
    }
}
