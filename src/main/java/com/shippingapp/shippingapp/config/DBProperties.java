package com.shippingapp.shippingapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.database")
public record DBProperties(
        String url,
        String username,
        String password,
        String schema,
        PoolConfiguration pool
) {

    public String getFullUrl() {
        if (url == null) {
            return null;
        }

        String baseUrl = url.endsWith("/") ? url : url + "/";
        return baseUrl + schema;
    }

    public record PoolConfiguration(
            int maxSize,
            int maxIdleMinutes
    ) {
    }
}