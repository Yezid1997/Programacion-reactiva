package com.shippingapp.shippingapp.repository;


import com.shippingapp.shippingapp.entity.PaqueteEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PaqueteRepository
        extends ReactiveCrudRepository<PaqueteEntity, Long> {

    Flux<PaqueteEntity> findByDespachoId(Long despachoId);
}