package com.shippingapp.shippingapp.job;

import com.shippingapp.shippingapp.service.ExpiracionService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class ExpiracionJob {

    private final Disposable suscripcion;

    public ExpiracionJob(ExpiracionService expiracionService) {
        this.suscripcion = Flux.interval(Duration.ofSeconds(30))
                .onBackpressureDrop()
                .concatMap(tick ->
                        expiracionService.expirarAsignacionesVencidas()
                                .onErrorResume(error -> {
                                    log.warn("Error en job de expiración: {}", error.getMessage());
                                    return Mono.just(0L);
                                })
                )
                .doOnCancel(() -> log.info("Job de expiración cancelado"))
                .doFinally(signal -> log.debug("Job de expiración finalizado: {}", signal))
                .subscribe();
    }

    @PreDestroy
    public void detener() {
        if (suscripcion != null && !suscripcion.isDisposed()) {
            suscripcion.dispose();
        }
    }
}
