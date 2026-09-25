package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.client.TransportistaClient;
import com.shippingapp.shippingapp.context.TrazaContext;
import com.shippingapp.shippingapp.dto.CreacionDespachoResult;
import com.shippingapp.shippingapp.dto.CrearDespachoRequest;
import com.shippingapp.shippingapp.dto.DatosLogisticos;
import com.shippingapp.shippingapp.dto.PaqueteRequest;
import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.entity.PaqueteEntity;
import com.shippingapp.shippingapp.exception.DespachoNoExisteException;
import com.shippingapp.shippingapp.exception.EstadoInvalidoException;
import com.shippingapp.shippingapp.exception.ZonaRiesgosaException;
import com.shippingapp.shippingapp.mapper.DespachoMapper;
import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.repository.DespachoRepository;
import com.shippingapp.shippingapp.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespachoService {

    private static final int UMBRAL_RIESGO = 80;
    private static final int MINUTOS_EXPIRACION = 15;

    private final DespachoRepository despachoRepository;
    private final PaqueteRepository paqueteRepository;
    private final AsignacionSaga asignacionSaga;
    private final TransportistaClient transportistaClient;
    private final DespachoMapper despachoMapper;
    private final TransactionalOperator transactionalOperator;
    private final CupoService cupoService;

    public Mono<CreacionDespachoResult> crear(
            CrearDespachoRequest request,
            String idempotencyKey
    ) {
        return Mono.deferContextual(ctx -> {
            String trazaId = TrazaContext.trazaId(ctx);
            log.info("[{}] Recibida solicitud de despacho para ciudad {}", trazaId, request.getCiudad());

            return buscarPorIdempotency(idempotencyKey)
                    .map(despacho -> new CreacionDespachoResult(despacho, false))
                    .switchIfEmpty(
                            crearNuevoDespacho(request, idempotencyKey, trazaId)
                                    .map(despacho -> new CreacionDespachoResult(despacho, true))
                    );
        });
    }

    public Mono<Despacho> buscarPorId(Long id) {
        return despachoRepository.findById(id)
                .switchIfEmpty(Mono.error(new DespachoNoExisteException(id)))
                .flatMap(this::mapearConPaquetes);
    }

    public Mono<Despacho> confirmar(Long id) {
        return despachoRepository.findById(id)
                .switchIfEmpty(Mono.error(new DespachoNoExisteException(id)))
                .flatMap(despacho -> {
                    if (despacho.getEstado() != EstadoDespacho.ASIGNADO) {
                        return Mono.error(
                                new EstadoInvalidoException(id, despacho.getEstado())
                        );
                    }
                    return paqueteRepository.findByDespachoId(id)
                            .concatMap(paquete ->
                                    cupoService.consumirReserva(
                                            paquete.getVehiculoId(),
                                            paquete.getPesoKg()
                                    )
                            )
                            .then(Mono.defer(() -> {
                                despacho.setEstado(EstadoDespacho.EN_RUTA);
                                despacho.setExpiraEn(null);
                                return despachoRepository.save(despacho);
                            }))
                            .flatMap(entity -> mapearConPaquetes(entity));
                });
    }

    private Mono<Despacho> crearNuevoDespacho(
            CrearDespachoRequest request,
            String idempotencyKey,
            String trazaId
    ) {
        return guardarRecibido(request, idempotencyKey, trazaId)
                .flatMap(despachoRecibido ->
                        asignacionSaga.reservarPaquetes(request.getPaquetes())
                                .flatMap(reservas ->
                                        transportistaClient.consultarDatosLogisticos(request.getCiudad())
                                                .flatMap(datos ->
                                                        procesarDatosLogisticos(
                                                                despachoRecibido,
                                                                request,
                                                                reservas,
                                                                datos
                                                        )
                                                )
                                                .onErrorResume(error -> {
                                                    if (error instanceof ZonaRiesgosaException) {
                                                        return Mono.error(error);
                                                    }
                                                    return asignacionSaga.compensar(reservas)
                                                            .then(Mono.error(error));
                                                })
                                )
                );
    }

    private Mono<Despacho> procesarDatosLogisticos(
            DespachoEntity despachoRecibido,
            CrearDespachoRequest request,
            List<ReservaCupo> reservas,
            DatosLogisticos datos
    ) {
        if (datos.scoreRiesgo() > UMBRAL_RIESGO) {
            return asignacionSaga.compensar(reservas)
                    .then(actualizarEstado(
                            despachoRecibido.getId(),
                            EstadoDespacho.RECHAZADO,
                            datos
                    ))
                    .then(Mono.error(new ZonaRiesgosaException(datos.scoreRiesgo())));
        }

        return asignarEnTransaccion(despachoRecibido, request, datos);
    }

    private Mono<DespachoEntity> guardarRecibido(
            CrearDespachoRequest request,
            String idempotencyKey,
            String trazaId
    ) {
        DespachoEntity entity = DespachoEntity.builder()
                .clienteId(request.getClienteId())
                .ciudad(request.getCiudad())
                .estado(EstadoDespacho.RECIBIDO)
                .trazaId(trazaId)
                .idempotencyKey(blankToNull(idempotencyKey))
                .creadoEn(OffsetDateTime.now())
                .build();

        return despachoRepository.save(entity);
    }

    private Mono<Despacho> asignarEnTransaccion(
            DespachoEntity despachoRecibido,
            CrearDespachoRequest request,
            DatosLogisticos datos
    ) {
        int pesoTotal = request.getPaquetes().stream()
                .mapToInt(PaqueteRequest::getPesoKg)
                .sum();
        BigDecimal total = datos.tarifa().add(BigDecimal.valueOf(pesoTotal));

        despachoRecibido.setEstado(EstadoDespacho.ASIGNADO);
        despachoRecibido.setTarifa(datos.tarifa());
        despachoRecibido.setTotal(total);
        despachoRecibido.setScoreRiesgo(datos.scoreRiesgo());
        despachoRecibido.setExpiraEn(
                OffsetDateTime.now().plusMinutes(MINUTOS_EXPIRACION)
        );

        Mono<DespachoEntity> asignacion = despachoRepository.save(despachoRecibido)
                .flatMap(despacho ->
                        Flux.fromIterable(request.getPaquetes())
                                .map(paquete -> PaqueteEntity.builder()
                                        .despachoId(despacho.getId())
                                        .vehiculoId(paquete.getVehiculoId())
                                        .pesoKg(paquete.getPesoKg())
                                        .build()
                                )
                                .flatMap(paqueteRepository::save)
                                .then(Mono.just(despacho))
                );

        return transactionalOperator.transactional(asignacion)
                .flatMap(this::mapearConPaquetes);
    }

    private Mono<Void> actualizarEstado(
            Long despachoId,
            EstadoDespacho estado,
            DatosLogisticos datos
    ) {
        return despachoRepository.findById(despachoId)
                .flatMap(despacho -> {
                    despacho.setEstado(estado);
                    despacho.setScoreRiesgo(datos.scoreRiesgo());
                    despacho.setTarifa(datos.tarifa());
                    return despachoRepository.save(despacho);
                })
                .then();
    }

    private Mono<Despacho> buscarPorIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.empty();
        }
        return despachoRepository.findByIdempotencyKey(idempotencyKey)
                .flatMap(this::mapearConPaquetes);
    }

    private Mono<Despacho> mapearConPaquetes(DespachoEntity entity) {
        return paqueteRepository.findByDespachoId(entity.getId())
                .collectList()
                .map(paquetes -> despachoMapper.toModel(entity, paquetes));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
