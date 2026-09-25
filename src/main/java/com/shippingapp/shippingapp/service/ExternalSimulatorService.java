package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.ClimaResponse;
import com.shippingapp.shippingapp.dto.RiesgoResponse;
import com.shippingapp.shippingapp.dto.TarifaResponse;
import com.shippingapp.shippingapp.external.SimulatorState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalSimulatorService {

    private static final Map<String, BigDecimal> TARIFAS_SIMULADAS = Map.of(
            "BOG", new BigDecimal("18500.00"),
            "MDE", new BigDecimal("17200.00"),
            "CLO", new BigDecimal("16800.00")
    );

    private final SimulatorState simulatorState;

    public Mono<TarifaResponse> consultarTarifa(String ciudad) {
        if (simulatorState.consumirFallaTarifa()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tarifa temporalmente no disponible"
            ));
        }
        BigDecimal tarifa = TARIFAS_SIMULADAS.getOrDefault(
                ciudad,
                new BigDecimal("16000.00")
        );
        return Mono.just(new TarifaResponse(ciudad, tarifa));
    }

    public Mono<ClimaResponse> consultarClima(String ciudad) {
        return Mono.delay(Duration.ofMillis(simulatorState.getLatenciaClimaMs()))
                .thenReturn(new ClimaResponse(ciudad, 6, "PARCIAL_NUBOSO"));
    }

    public Mono<RiesgoResponse> consultarRiesgo(String ciudad) {
        return Mono.delay(Duration.ofMillis(simulatorState.getLatenciaRiesgoMs()))
                .thenReturn(new RiesgoResponse(
                        ciudad,
                        simulatorState.getScoreRiesgo()
                ));
    }
}
