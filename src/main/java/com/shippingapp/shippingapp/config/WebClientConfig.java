package com.shippingapp.shippingapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(AppProperties appProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.baseUrl())
                .build();
    }
}
