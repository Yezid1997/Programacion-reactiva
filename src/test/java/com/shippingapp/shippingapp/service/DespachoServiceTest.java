package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.client.TransportistaClient;
import com.shippingapp.shippingapp.dto.CrearDespachoRequest;
import com.shippingapp.shippingapp.dto.DatosLogisticos;
import com.shippingapp.shippingapp.dto.PaqueteRequest;
import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.entity.PaqueteEntity;
import com.shippingapp.shippingapp.exception.ZonaRiesgosaException;
import com.shippingapp.shippingapp.mapper.DespachoMapper;
import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.repository.DespachoRepository;
import com.shippingapp.shippingapp.repository.PaqueteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private DespachoRepository despachoRepository;
    @Mock
    private PaqueteRepository paqueteRepository;
    @Mock
    private AsignacionSaga asignacionSaga;
    @Mock
    private TransportistaClient transportistaClient;
    @Mock
    private DespachoMapper despachoMapper;
    @Mock
    private TransactionalOperator transactionalOperator;
    @Mock
    private CupoService cupoService;
    @Mock
    private DespachoEventBus eventBus;

    @InjectMocks
    private DespachoService despachoService;

    @BeforeEach
    void stubsComunes() {
        lenient().when(eventBus.publicar(any())).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        );
        lenient().when(paqueteRepository.findByDespachoId(any())).thenReturn(Flux.empty());
        lenient().when(despachoMapper.toModel(any(), any())).thenAnswer(invocation -> {
            DespachoEntity entity = invocation.getArgument(0);
            return Despacho.builder()
                    .id(entity.getId())
                    .estado(entity.getEstado())
                    .ciudad(entity.getCiudad())
                    .trazaId(entity.getTrazaId())
                    .scoreRiesgo(entity.getScoreRiesgo())
                    .build();
        });
    }

    @Test
    void rechazaZonaRiesgosaYCompensaCupo() {
        var request = requestBog();
        var recibido = DespachoEntity.builder()
                .id(10L)
                .estado(EstadoDespacho.RECIBIDO)
                .ciudad("BOG")
                .build();
        var reservas = List.of(new ReservaCupo(1L, 50));
        var datos = new DatosLogisticos(new BigDecimal("15000"), 6, "LLUVIA", 95);

        when(despachoRepository.save(any(DespachoEntity.class))).thenReturn(Mono.just(recibido));
        when(asignacionSaga.reservarPaquetes(anyList())).thenReturn(Mono.just(reservas));
        when(transportistaClient.consultarDatosLogisticos("BOG")).thenReturn(Mono.just(datos));
        when(asignacionSaga.compensar(reservas)).thenReturn(Mono.empty());
        when(despachoRepository.findById(10L)).thenReturn(Mono.just(recibido));

        StepVerifier.create(crear(request, null))
                .expectError(ZonaRiesgosaException.class)
                .verify();

        verify(asignacionSaga).compensar(reservas);
        verify(transactionalOperator, never()).transactional(any(Mono.class));

        ArgumentCaptor<DespachoEntity> captor = ArgumentCaptor.forClass(DespachoEntity.class);
        verify(despachoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getTrazaId()).isEqualTo("test-traza");
        assertThat(captor.getAllValues().get(0).getEstado()).isEqualTo(EstadoDespacho.RECIBIDO);
    }

    @Test
    void asignaCuandoElScoreNoSupera80() {
        var request = requestBog();
        var reservas = List.of(new ReservaCupo(1L, 50));
        var datos = new DatosLogisticos(new BigDecimal("15000.00"), 6, "SOLEADO", 80);

        when(despachoRepository.save(any(DespachoEntity.class))).thenAnswer(invocation -> {
            DespachoEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(10L);
            }
            return Mono.just(entity);
        });
        when(asignacionSaga.reservarPaquetes(anyList())).thenReturn(Mono.just(reservas));
        when(transportistaClient.consultarDatosLogisticos("BOG")).thenReturn(Mono.just(datos));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paqueteRepository.save(any(PaqueteEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(crear(request, null))
                .assertNext(result -> {
                    assertThat(result.nuevo()).isTrue();
                    assertThat(result.despacho().getEstado()).isEqualTo(EstadoDespacho.ASIGNADO);
                    assertThat(result.despacho().getScoreRiesgo()).isEqualTo(80);
                })
                .verifyComplete();

        verify(asignacionSaga, never()).compensar(anyList());
        verify(transactionalOperator).transactional(any(Mono.class));
        verify(eventBus).publicar(any(Despacho.class));
    }

    @Test
    void idempotenciaDevuelveElDespachoSinVolverAReservar() {
        var existente = DespachoEntity.builder()
                .id(7L)
                .estado(EstadoDespacho.ASIGNADO)
                .ciudad("BOG")
                .build();
        when(despachoRepository.findByIdempotencyKey("K1")).thenReturn(Mono.just(existente));

        StepVerifier.create(crear(requestBog(), "K1"))
                .assertNext(result -> {
                    assertThat(result.nuevo()).isFalse();
                    assertThat(result.despacho().getId()).isEqualTo(7L);
                    assertThat(result.despacho().getEstado()).isEqualTo(EstadoDespacho.ASIGNADO);
                })
                .verifyComplete();

        verify(asignacionSaga, never()).reservarPaquetes(anyList());
    }

    private Mono<com.shippingapp.shippingapp.dto.CreacionDespachoResult> crear(
            CrearDespachoRequest request,
            String idempotencyKey
    ) {
        return despachoService.crear(request, idempotencyKey)
                .contextWrite(Context.of("trazaId", "test-traza"));
    }

    private static CrearDespachoRequest requestBog() {
        return CrearDespachoRequest.builder()
                .clienteId(1L)
                .ciudad("BOG")
                .paquetes(List.of(
                        PaqueteRequest.builder().vehiculoId(1L).pesoKg(50).build()
                ))
                .build();
    }
}
