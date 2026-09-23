package com.shippingapp.shippingapp.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import java.time.Duration;

@Configuration
public class DBConfiguration {

    private final DBProperties dbProperties;

    public DBConfiguration(DBProperties dbProperties) {
        this.dbProperties = dbProperties;
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public ConnectionFactory connectionFactory() {

        assert dbProperties.getFullUrl() != null;

        ConnectionFactory connectionFactory = ConnectionFactoryBuilder.withUrl(dbProperties.getFullUrl())
                .username(dbProperties.userName())
                .password(dbProperties.password())
                .build();

        ConnectionPoolConfiguration connectionPoolConfiguration = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                .maxSize(dbProperties.pool().maxSize())
                .maxIdleTime(Duration.ofMinutes(dbProperties.pool().maxIdleMinutes()))
                .build();

        return new ConnectionPool(connectionPoolConfiguration);
    }
}
