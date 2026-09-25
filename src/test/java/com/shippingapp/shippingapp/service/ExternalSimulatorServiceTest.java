package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.external.SimulatorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class ExternalSimulatorServiceTest {

    private SimulatorState simulatorState;
    private ExternalSimulatorService externalSimulatorService;

    @BeforeEach
    void setUp() {
        simulatorState = new SimulatorState();
        externalSimulatorService = new ExternalSimulatorService(simulatorState);
    }

    @Test
    void simulaFallasIntermitentesDeTarifa() {
        simulatorState.actualizar(2, null, null, null);

        StepVerifier.create(externalSimulatorService.consultarTarifa("BOG"))
                .expectError(ResponseStatusException.class)
                .verify();
        StepVerifier.create(externalSimulatorService.consultarTarifa("BOG"))
                .expectError(ResponseStatusException.class)
                .verify();
        StepVerifier.create(externalSimulatorService.consultarTarifa("BOG"))
                .expectNextMatches(t -> t.ciudad().equals("BOG"))
                .verifyComplete();
    }
}
