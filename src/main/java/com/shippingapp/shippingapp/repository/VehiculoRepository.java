package com.shippingapp.shippingapp.repository;


import com.shippingapp.shippingapp.entity.VehiculoEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface VehiculoRepository
        extends ReactiveCrudRepository<VehiculoEntity, Long> {

    Mono<VehiculoEntity> findByPlaca(String placa);
}