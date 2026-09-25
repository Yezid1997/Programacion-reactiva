package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.PaqueteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionSaga {

    private final CupoService cupoService;

    /**
     * Reserva cupo paquete a paquete (orden importa para compensar de atrás hacia adelante).
     * Si un paquete falla, libera todo lo ya reservado antes de propagar el error.
     */
    public Mono<List<ReservaCupo>> reservarPaquetes(List<PaqueteRequest> paquetes) {
        List<ReservaCupo> reservasExitosas = new ArrayList<>();

        return Flux.fromIterable(paquetes)
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
                        compensar(reservasExitosas).then(Mono.error(error))
                );
    }

    public Mono<Void> compensar(List<ReservaCupo> reservas) {
        if (reservas.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(reservas)
                .concatMap(reserva ->
                        cupoService.liberar(
                                reserva.vehiculoId(),
                                reserva.pesoKg()
                        )
                )
                .then();
    }
}
