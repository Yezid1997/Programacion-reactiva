package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.dto.ClimaResponse;
import com.shippingapp.shippingapp.dto.RiesgoResponse;
import com.shippingapp.shippingapp.dto.SimulatorConfigRequest;
import com.shippingapp.shippingapp.dto.SimulatorConfigResponse;
import com.shippingapp.shippingapp.dto.TarifaResponse;
import com.shippingapp.shippingapp.external.SimulatorState;
import com.shippingapp.shippingapp.service.ExternalSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/external")
@RequiredArgsConstructor
public class ExternalController {

    private final ExternalSimulatorService externalSimulatorService;
    private final SimulatorState simulatorState;

    @GetMapping("/tarifas/{ciudad}")
    public Mono<TarifaResponse> tarifa(@PathVariable String ciudad) {
        return externalSimulatorService.consultarTarifa(ciudad);
    }

    @GetMapping("/clima/{ciudad}")
    public Mono<ClimaResponse> clima(@PathVariable String ciudad) {
        return externalSimulatorService.consultarClima(ciudad);
    }

    @GetMapping("/riesgo/{ciudad}")
    public Mono<RiesgoResponse> riesgo(@PathVariable String ciudad) {
        return externalSimulatorService.consultarRiesgo(ciudad);
    }

    @GetMapping("/simulator")
    public Mono<SimulatorConfigResponse> obtenerConfiguracionSimulador() {
        return Mono.fromSupplier(() -> SimulatorConfigResponse.builder()
                .fallasTarifa(simulatorState.getFallasTarifa())
                .latenciaClimaMs(simulatorState.getLatenciaClimaMs())
                .latenciaRiesgoMs(simulatorState.getLatenciaRiesgoMs())
                .scoreRiesgo(simulatorState.getScoreRiesgo())
                .build());
    }

    @PutMapping("/simulator")
    public Mono<SimulatorConfigResponse> actualizarConfiguracionSimulador(
            @RequestBody SimulatorConfigRequest request
    ) {
        return Mono.fromRunnable(() ->
                simulatorState.actualizar(
                        request.getFallasTarifa(),
                        request.getLatenciaClimaMs(),
                        request.getLatenciaRiesgoMs(),
                        request.getScoreRiesgo()
                )
        ).then(obtenerConfiguracionSimulador());
    }

    @DeleteMapping("/simulator")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> restablecerSimulador() {
        return Mono.fromRunnable(simulatorState::reset).then();
    }
}
