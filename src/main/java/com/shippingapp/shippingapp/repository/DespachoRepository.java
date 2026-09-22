package com.shippingapp.shippingapp.repository;


import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface DespachoRepository
        extends ReactiveCrudRepository<DespachoEntity, Long> {

    Mono<DespachoEntity> findByIdempotencyKey(String idempotencyKey);

    Flux<DespachoEntity> findByEstadoAndExpiraEnBefore(
            EstadoDespacho estado,
            OffsetDateTime instante
    );
}