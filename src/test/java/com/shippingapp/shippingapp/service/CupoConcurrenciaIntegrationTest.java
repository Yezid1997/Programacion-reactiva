package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.entity.VehiculoEntity;
import com.shippingapp.shippingapp.exception.CupoInsuficienteException;
import com.shippingapp.shippingapp.repository.VehiculoRepository;
import com.shippingapp.shippingapp.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIf("com.shippingapp.shippingapp.support.PostgresTestSupport#disponible")
class CupoConcurrenciaIntegrationTest {

    private static final long VEHICULO_ID = 50L;
    private static final int CUPO = 500;
    private static final int PESO = 40;
    private static final int PETICIONES = 20;

    @Autowired
    private CupoService cupoService;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @BeforeEach
    void prepararVehiculo() {
        VehiculoEntity alta = VehiculoEntity.builder()
                .id(VEHICULO_ID)
                .placa("CONC50")
                .ciudad("BOG")
                .cupoKg(CUPO)
                .reservadoKg(0)
                .build();

        StepVerifier.create(
                vehiculoRepository.findById(VEHICULO_ID)
                        .flatMap(existente -> {
                            existente.setCupoKg(CUPO);
                            existente.setReservadoKg(0);
                            existente.setPlaca("CONC50");
                            return vehiculoRepository.save(existente);
                        })
                        .switchIfEmpty(vehiculoRepository.save(alta))
        ).expectNextCount(1).verifyComplete();
    }

    @Test
    void peticionesEnParaleloNoDejanCupoNegativo() {
        int exitosasEsperadas = CUPO / PESO;

        StepVerifier.create(
                Flux.range(1, PETICIONES)
                        .flatMap(i -> cupoService.reservar(VEHICULO_ID, PESO)
                                        .thenReturn(1)
                                        .onErrorResume(CupoInsuficienteException.class, error -> Mono.just(0)),
                                PETICIONES
                        )
                        .reduce(0, Integer::sum)
        ).assertNext(exitosas -> assertThat(exitosas).isEqualTo(exitosasEsperadas))
                .verifyComplete();

        StepVerifier.create(vehiculoRepository.findById(VEHICULO_ID))
                .assertNext(vehiculo -> {
                    assertThat(vehiculo.getCupoKg()).isGreaterThanOrEqualTo(0);
                    assertThat(vehiculo.getReservadoKg()).isGreaterThanOrEqualTo(0);
                    assertThat(vehiculo.getCupoKg()).isEqualTo(CUPO - exitosasEsperadas * PESO);
                    assertThat(vehiculo.getReservadoKg()).isEqualTo(exitosasEsperadas * PESO);
                })
                .verifyComplete();
    }
}
