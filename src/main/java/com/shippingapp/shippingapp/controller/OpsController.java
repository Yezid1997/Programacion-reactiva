package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.service.DespachoEventBus;
import com.shippingapp.shippingapp.support.ReactiveSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {

    private final DespachoEventBus eventBus;

    /**
     * Un solo intervalo para todos los monitores. Sin suscriptores, refCount
     * lo apaga: no queda un timer vivo después de que el último cliente se va.
     */
    private final Flux<ServerSentEvent<Despacho>> pulsoCompartido =
            ReactiveSupport.<Despacho>latidos(ReactiveSupport.PERIODO_LATIDO).publish().refCount();

    @GetMapping(value = "/tablero", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Despacho>> getDashBoard() {
        Flux<ServerSentEvent<Despacho>> cambios = eventBus.tablero()
                .distinctUntilChanged(despacho -> despacho.getId() + ":" + despacho.getEstado())
                .map(ShippingController::aEvento);
        return ReactiveSupport.alDesconectar(Flux.merge(cambios, pulsoCompartido));
    }
}
