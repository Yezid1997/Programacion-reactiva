package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.dto.CrearDespachoRequest;
import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.service.DespachoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/despachos")
@RequiredArgsConstructor
public class ShippingController {

    private final DespachoService despachoService;

    @PostMapping
    public Mono<ResponseEntity<Despacho>> create(
            @Valid @RequestBody CrearDespachoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return despachoService.crear(request, idempotencyKey)
                .map(result -> result.nuevo()
                        ? ResponseEntity.status(HttpStatus.CREATED).body(result.despacho())
                        : ResponseEntity.ok(result.despacho()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Despacho>> getShipment(@PathVariable Long id) {
        return despachoService.buscarPorId(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/confirm")
    public Mono<ResponseEntity<Despacho>> confirm(@PathVariable Long id) {
        return despachoService.confirmar(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Despacho>> events(@PathVariable Long id) {
        return Flux.empty();
    }
}
