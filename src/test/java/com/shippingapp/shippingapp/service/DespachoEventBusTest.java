package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

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
}
