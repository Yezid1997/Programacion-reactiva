package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.model.Vehiculo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vehiculos")
public class VehicleController {
    @PostMapping(value = "/bulk", consumes = MediaType.APPLICATION_NDJSON_VALUE)
    public Mono<ResponseEntity<Vehiculo>> loadVehicles(@RequestBody Flux<Vehiculo> vehicles) {
        return Mono.just(null);
    }
}
