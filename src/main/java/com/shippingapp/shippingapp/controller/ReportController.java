package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.dto.ReporteCiudad;
import com.shippingapp.shippingapp.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReporteService reporteService;

    @GetMapping("/ciudades")
    public Mono<List<ReporteCiudad>> getCities() {
        return reporteService.totalesPorCiudad();
    }

    @GetMapping(value = "/ciudades/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<ReporteCiudad> getStreamCities() {
        return reporteService.streamAcumulado();
    }
}
