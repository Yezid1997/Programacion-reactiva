package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.service.DespachoEventBus;
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

    @GetMapping(value = "/tablero", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Despacho>> getDashBoard() {
        return eventBus.tablero()
                .map(ShippingController::aEvento);
    }
}
