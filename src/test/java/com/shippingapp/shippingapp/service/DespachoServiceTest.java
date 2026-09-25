package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.client.TransportistaClient;
import com.shippingapp.shippingapp.dto.CrearDespachoRequest;
import com.shippingapp.shippingapp.dto.DatosLogisticos;
import com.shippingapp.shippingapp.dto.PaqueteRequest;
import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.exception.ZonaRiesgosaException;
import com.shippingapp.shippingapp.mapper.DespachoMapper;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.repository.DespachoRepository;
import com.shippingapp.shippingapp.repository.PaqueteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    @InjectMocks
    private DespachoService despachoService;

    @Test
    void rechazaZonaRiesgosaYCompensaCupo() {
        var request = CrearDespachoRequest.builder()
                .clienteId(1L)
                .ciudad("BOG")
                .paquetes(List.of(
                        PaqueteRequest.builder().vehiculoId(1L).pesoKg(50).build()
                ))
                .build();

        var recibido = DespachoEntity.builder()
                .id(10L)
                .estado(EstadoDespacho.RECIBIDO)
                .build();
        var reservas = List.of(new ReservaCupo(1L, 50));
        var datos = new DatosLogisticos(
                new BigDecimal("15000"),
                6,
                "LLUVIA",
                95
        );

        when(despachoRepository.save(any(DespachoEntity.class))).thenReturn(Mono.just(recibido));
        when(asignacionSaga.reservarPaquetes(anyList())).thenReturn(Mono.just(reservas));
        when(transportistaClient.consultarDatosLogisticos("BOG")).thenReturn(Mono.just(datos));
        when(asignacionSaga.compensar(reservas)).thenReturn(Mono.empty());
        when(despachoRepository.findById(10L)).thenReturn(Mono.just(recibido));
        when(despachoRepository.save(recibido)).thenReturn(Mono.just(recibido));

        StepVerifier.create(
                        despachoService.crear(request, null)
                                .contextWrite(Context.of("trazaId", "test-traza"))
                )
                .expectError(ZonaRiesgosaException.class)
                .verify();

        verify(asignacionSaga).compensar(reservas);
        verify(transactionalOperator, never()).transactional(any(Mono.class));
    }
}
