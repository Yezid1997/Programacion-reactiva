package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.PaqueteRequest;
import com.shippingapp.shippingapp.exception.CupoInsuficienteException;
import com.shippingapp.shippingapp.repository.VehiculoRepository;
import com.shippingapp.shippingapp.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIf("com.shippingapp.shippingapp.support.PostgresTestSupport#disponible")
class CupoAsignacionIntegrationTest {

    @Autowired
    private AsignacionSaga asignacionSaga;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @BeforeEach
    void resetVehiculoUno() {
        StepVerifier.create(
                vehiculoRepository.findById(1L)
                        .flatMap(v -> {
                            v.setCupoKg(500);
                            v.setReservadoKg(0);
                            return vehiculoRepository.save(v);
                        })
        ).verifyComplete();
    }

    @Test
    void mantieneCupoOriginalTrasFalloYCompensacion() {
        var paquetes = List.of(
                PaqueteRequest.builder().vehiculoId(1L).pesoKg(120).build(),
                PaqueteRequest.builder().vehiculoId(1L).pesoKg(500).build()
        );

        StepVerifier.create(asignacionSaga.reservarPaquetes(paquetes))
                .expectError(CupoInsuficienteException.class)
                .verify();

        StepVerifier.create(vehiculoRepository.findById(1L))
                .assertNext(v -> {
                    assertThat(v.getCupoKg()).isEqualTo(500);
                    assertThat(v.getReservadoKg()).isEqualTo(0);
                })
                .verifyComplete();
    }
}
