package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.dto.CargaMasivaResponse;
import com.shippingapp.shippingapp.dto.CrearVehiculoRequest;
import com.shippingapp.shippingapp.model.Vehiculo;
import com.shippingapp.shippingapp.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehicleController {

    private final VehiculoService vehiculoService;

    @GetMapping
    public Flux<Vehiculo> listar() {
        return vehiculoService.listar();
    }

    @GetMapping("/{id}")
    public Mono<Vehiculo> buscarPorId(@PathVariable Long id) {
        return vehiculoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Vehiculo> crear(
            @Valid @RequestBody CrearVehiculoRequest request
    ) {
        return vehiculoService.crear(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminar(@PathVariable Long id) {
        return vehiculoService.eliminar(id);
    }

    @PostMapping(
            value = "/bulk",
            consumes = MediaType.APPLICATION_NDJSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<CargaMasivaResponse> cargarMasivo(
            @RequestBody Flux<Vehiculo> vehiculos
    ) {
        return vehiculoService.cargarMasivo(vehiculos);
    }
}