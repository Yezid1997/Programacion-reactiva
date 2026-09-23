package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.CrearVehiculoRequest;
import com.shippingapp.shippingapp.exception.VehiculoNoExisteException;
import com.shippingapp.shippingapp.mapper.VehiculoMapper;
import com.shippingapp.shippingapp.model.Vehiculo;
import com.shippingapp.shippingapp.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final VehiculoMapper vehiculoMapper;

    public Flux<Vehiculo> listar() {
        return vehiculoRepository.findAll()
                .map(vehiculoMapper::toModel);
    }

    public Mono<Vehiculo> buscarPorId(Long id) {
        return vehiculoRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new VehiculoNoExisteException(id)
                ))
                .map(vehiculoMapper::toModel);
    }

    public Mono<Vehiculo> crear(CrearVehiculoRequest request) {
        return Mono.defer(() ->
                vehiculoRepository.save(
                        vehiculoMapper.toEntity(request)
                )
        ).map(vehiculoMapper::toModel);
    }

    public Mono<Void> eliminar(Long id) {
        return vehiculoRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new VehiculoNoExisteException(id)
                ))
                .flatMap(vehiculoRepository::delete);
    }
}
