package com.shippingapp.shippingapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @GetMapping("/ciudades")
    public Mono<?> getCities(){
        return Mono.just(null);
    }

    @GetMapping("/ciudades/stream")
    public Mono<?> getStreamCities(){
        return Mono.just(null);
    }
}
