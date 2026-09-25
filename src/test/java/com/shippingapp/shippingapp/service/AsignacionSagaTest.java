package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.PaqueteRequest;
import com.shippingapp.shippingapp.entity.VehiculoEntity;
import com.shippingapp.shippingapp.exception.CupoInsuficienteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionSagaTest {

    @Mock
    private CupoService cupoService;

    @InjectMocks
    private AsignacionSaga asignacionSaga;

    private static final VehiculoEntity VEHICULO_OK = VehiculoEntity.builder()
            .id(1L)
            .cupoKg(380)
            .reservadoKg(120)
            .build();

    @Test
    void reservaTodosLosPaquetesCuandoHayCupo() {
        var paquetes = List.of(
                PaqueteRequest.builder().vehiculoId(1L).pesoKg(50).build(),
                PaqueteRequest.builder().vehiculoId(2L).pesoKg(30).build()
        );

        when(cupoService.reservar(1L, 50)).thenReturn(Mono.just(VEHICULO_OK));
        when(cupoService.reservar(2L, 30)).thenReturn(Mono.just(VEHICULO_OK));

        StepVerifier.create(asignacionSaga.reservarPaquetes(paquetes))
                .expectNextMatches(reservas ->
                        reservas.size() == 2
                                && reservas.get(0).vehiculoId().equals(1L)
                                && reservas.get(1).vehiculoId().equals(2L)
                )
                .verifyComplete();

        verify(cupoService, never()).liberar(eq(1L), eq(50));
    }

    @Test
    void compensaReservasPreviasCuandoFallaUnPaqueteIntermedio() {
        var paquetes = List.of(
                PaqueteRequest.builder().vehiculoId(1L).pesoKg(50).build(),
                PaqueteRequest.builder().vehiculoId(1L).pesoKg(999).build()
        );

        when(cupoService.reservar(1L, 50)).thenReturn(Mono.just(VEHICULO_OK));
        when(cupoService.reservar(1L, 999))
                .thenReturn(Mono.error(new CupoInsuficienteException(1L, 999)));
        when(cupoService.liberar(1L, 50)).thenReturn(Mono.just(VEHICULO_OK));

        StepVerifier.create(asignacionSaga.reservarPaquetes(paquetes))
                .expectError(CupoInsuficienteException.class)
                .verify();

        verify(cupoService).liberar(1L, 50);
    }
}
