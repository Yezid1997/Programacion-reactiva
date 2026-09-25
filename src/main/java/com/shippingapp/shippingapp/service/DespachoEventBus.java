package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.model.Despacho;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@Slf4j
public class DespachoEventBus {

    private final Sinks.Many<Despacho> bus = Sinks.many()
            .multicast()
            .onBackpressureBuffer(1024, false);

    public Mono<Despacho> publicar(Despacho despacho) {
        return Mono.fromSupplier(() -> {
            Sinks.EmitResult resultado = bus.tryEmitNext(despacho);
            if (resultado.isFailure()
                    && resultado != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                log.warn(
                        "Evento de despacho {} ({}) no entregado al bus: {}",
                        despacho.getId(),
                        despacho.getEstado(),
                        resultado
                );
            }
            return despacho;
        });
    }

    /**
     * Tablero caliente: todos los suscriptores comparten el mismo multicast.
     */
    public Flux<Despacho> tablero() {
        return bus.asFlux();
    }

    public Flux<Despacho> porDespacho(Long despachoId) {
        return bus.asFlux()
                .filter(despacho -> despachoId.equals(despacho.getId()));
    }
}
