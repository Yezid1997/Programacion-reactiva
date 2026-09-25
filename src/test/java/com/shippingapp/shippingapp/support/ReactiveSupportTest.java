package com.shippingapp.shippingapp.support;

import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveSupportTest {

    @Test
    void elHeartbeatNoImpideCerrarCuandoLosDatosTerminan() {
        ServerSentEvent<String> dato = ServerSentEvent.<String>builder("ok")
                .event("ASIGNADO")
                .build();

        StepVerifier.create(
                        ReactiveSupport.fundirConHeartbeat(Flux.just(dato), Duration.ofHours(1))
                )
                .expectNext(dato)
                .verifyComplete();
    }

    @Test
    void alDesconectarAvisaYLibera() {
        AtomicReference<SignalType> senal = new AtomicReference<>();

        StepVerifier.create(
                        ReactiveSupport.alDesconectar(Flux.never(), senal::set)
                )
                .thenCancel()
                .verify();

        assertThat(senal.get()).isEqualTo(SignalType.CANCEL);
    }
}
