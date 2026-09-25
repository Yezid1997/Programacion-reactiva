package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DespachoEventBusTest {

    @Test
    void multicastAlimentaTableroYStreamDelDespacho() {
        DespachoEventBus bus = new DespachoEventBus();
        Despacho evento = Despacho.builder()
                .id(4L)
                .estado(EstadoDespacho.ASIGNADO)
                .ciudad("BOG")
                .build();
        List<Despacho> tablero = new CopyOnWriteArrayList<>();
        List<Despacho> detalle = new CopyOnWriteArrayList<>();
        List<Despacho> otro = new CopyOnWriteArrayList<>();

        bus.tablero().subscribe(tablero::add);
        bus.porDespacho(4L).subscribe(detalle::add);
        bus.porDespacho(99L).subscribe(otro::add);

        StepVerifier.create(bus.publicar(evento))
                .expectNext(evento)
                .verifyComplete();

        assertThat(tablero).containsExactly(evento);
        assertThat(detalle).containsExactly(evento);
        assertThat(otro).isEmpty();
    }

    @Test
    void consumidorLentoDelTableroSoloRecibeElUltimoEstado() {
        DespachoEventBus bus = new DespachoEventBus();
        List<Despacho> vistos = new ArrayList<>();
        BaseSubscriber<Despacho> lento = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(Despacho value) {
                vistos.add(value);
            }
        };
        bus.tablero().subscribe(lento);

        Despacho primero = despacho(1L);
        Despacho segundo = despacho(2L);
        Despacho tercero = despacho(3L);
        bus.publicar(primero).block();
        bus.publicar(segundo).block();
        bus.publicar(tercero).block();

        assertThat(vistos).containsExactly(primero);
        lento.request(1);
        assertThat(vistos).containsExactly(primero, tercero);
    }

    private static Despacho despacho(Long id) {
        return Despacho.builder()
                .id(id)
                .estado(EstadoDespacho.ASIGNADO)
                .ciudad("BOG")
                .build();
    }
}
