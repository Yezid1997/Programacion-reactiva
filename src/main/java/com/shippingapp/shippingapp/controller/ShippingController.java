package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.dto.CrearDespachoRequest;
import com.shippingapp.shippingapp.model.Despacho;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/despachos")
public class ShippingController {

    @PostMapping
    public Mono<ResponseEntity<Despacho>> create(
            @Valid  @RequestBody CrearDespachoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey)
    {
        return Mono.just(null);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Despacho>> getShipment(@PathVariable Long id) {
        return Mono.just(null);
    }

    @PostMapping("/{id}/confirm")
    public Mono<ResponseEntity<Despacho>> confirm(@PathVariable Long id) {
        return Mono.just(null);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Despacho>> events(@PathVariable Long id) {
        return Flux.just(null);
    }
}
