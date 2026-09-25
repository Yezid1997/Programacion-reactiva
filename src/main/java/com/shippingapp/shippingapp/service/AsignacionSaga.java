package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.PaqueteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionSaga {

    private final CupoService cupoService;

    /**
     * Reserva cupo paquete a paquete. Si uno falla a mitad, devuelve el cupo
     * ya tomado en orden inverso (el último reservado se libera primero).
     */
    public Mono<List<ReservaCupo>> reservarPaquetes(List<PaqueteRequest> paquetes) {
        return Mono.defer(() -> {
            List<ReservaCupo> reservasExitosas = new ArrayList<>();

            return Mono.just(paquetes)
                    .flatMapIterable(lista -> lista)
                    // concatMap, no flatMap: el orden de reserva es el orden de
                    // compensación. En paralelo, dos paquetes del mismo vehículo
                    // pisarían el UPDATE y la saga no sabría qué devolver.
                    .concatMap(paquete ->
                            cupoService.reservar(
                                            paquete.getVehiculoId(),
                                            paquete.getPesoKg()
                                    )
                                    .map(vehiculo -> {
                                        ReservaCupo reserva = new ReservaCupo(
                                                paquete.getVehiculoId(),
                                                paquete.getPesoKg()
                                        );
                                        reservasExitosas.add(reserva);
                                        return reserva;
                                    })
                    )
                    .collectList()
                    .onErrorResume(error ->
                            compensar(List.copyOf(reservasExitosas))
                                    .onErrorResume(compensacion -> {
                                        error.addSuppressed(compensacion);
                                        return Mono.empty();
                                    })
                                    .then(Mono.error(error))
                    );
        });
    }

    public Mono<Void> compensar(List<ReservaCupo> reservas) {
        if (reservas == null || reservas.isEmpty()) {
            return Mono.empty();
        }
        List<ReservaCupo> reverso = new ArrayList<>(reservas);
        Collections.reverse(reverso);
        return Flux.fromIterable(reverso)
                .concatMap(reserva ->
                        cupoService.liberar(
                                reserva.vehiculoId(),
                                reserva.pesoKg()
                        )
                )
                .then();
    }
}
