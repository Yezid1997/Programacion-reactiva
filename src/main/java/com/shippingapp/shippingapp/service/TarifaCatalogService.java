package com.shippingapp.shippingapp.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class TarifaCatalogService {

    private static final BigDecimal TARIFA_DEFAULT = new BigDecimal("12000.00");

    private static final Map<String, BigDecimal> TARIFAS_BASE = Map.of(
            "BOG", new BigDecimal("15000.00"),
            "MDE", new BigDecimal("13500.00"),
            "CLO", new BigDecimal("12800.00")
    );

    public Mono<BigDecimal> tarifaBase(String ciudad) {
        return Mono.fromSupplier(() ->
                TARIFAS_BASE.getOrDefault(ciudad, TARIFA_DEFAULT)
        );
    }
}
