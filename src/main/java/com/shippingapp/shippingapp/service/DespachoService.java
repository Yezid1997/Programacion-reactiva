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
    private final DespachoEventBus eventBus;

    public Mono<CreacionDespachoResult> crear(
            CrearDespachoRequest request,
            String idempotencyKey
    ) {
        return TrazaContext.info(
                        log,
                        "Recibida solicitud de despacho para ciudad {}",
                        request.getCiudad()
                )
                .then(resolver(request, idempotencyKey));
    }

    public Mono<Despacho> buscarPorId(Long id) {
        return despachoRepository.findById(id)
                .switchIfEmpty(Mono.error(new DespachoNoExisteException(id)))
                .flatMap(this::mapearConPaquetes);
    }

    public Mono<Despacho> confirmar(Long id) {
        return TrazaContext.info(log, "Confirmando despacho {}", id)
                .then(despachoRepository.findById(id))
                .switchIfEmpty(Mono.error(new DespachoNoExisteException(id)))
                .flatMap(despacho -> {
                    if (despacho.getEstado() != EstadoDespacho.ASIGNADO) {
                        return Mono.error(
                                new EstadoInvalidoException(id, despacho.getEstado())
                        );
                    }
                    Mono<DespachoEntity> confirmacion = paqueteRepository.findByDespachoId(id)
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
                            }));

                    return transactionalOperator.transactional(confirmacion)
                            .flatMap(this::mapearConPaquetes)
                            .flatMap(eventBus::publicar);
                });
    }

    private Mono<CreacionDespachoResult> resolver(
            CrearDespachoRequest request,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return crearNuevoDespacho(request, null)
                    .map(despacho -> new CreacionDespachoResult(despacho, true));
        }

        return despachoRepository.findByIdempotencyKey(idempotencyKey)
                .flatMap(existente -> {
                    if (existente.getEstado() == EstadoDespacho.RECIBIDO) {
                        return continuarAsignacion(existente, request)
                                .map(despacho -> new CreacionDespachoResult(despacho, true));
                    }
                    return mapearConPaquetes(existente)
                            .map(despacho -> new CreacionDespachoResult(despacho, false));
                })
                .switchIfEmpty(Mono.defer(() ->
                        crearNuevoDespacho(request, idempotencyKey)
                                .map(despacho -> new CreacionDespachoResult(despacho, true))
                ));
    }

    private Mono<Despacho> crearNuevoDespacho(
            CrearDespachoRequest request,
            String idempotencyKey
    ) {
        return guardarRecibido(request, idempotencyKey)
                .flatMap(despachoRecibido -> continuarAsignacion(despachoRecibido, request));
    }

    private Mono<Despacho> continuarAsignacion(
            DespachoEntity despachoRecibido,
            CrearDespachoRequest request
    ) {
        return asignacionSaga.reservarPaquetes(request.getPaquetes())
                .flatMap(reservas ->
                        transportistaClient.consultarDatosLogisticos(request.getCiudad())
                                .flatMap(datos ->
                                        procesarDatosLogisticos(
                                                despachoRecibido,
                                                request,
                                                datos
                                        )
                                )
                                .flatMap(eventBus::publicar)
                                .onErrorResume(error ->
                                        asignacionSaga.compensar(reservas)
                                                .onErrorResume(compensacion -> {
                                                    error.addSuppressed(compensacion);
                                                    return Mono.empty();
                                                })
                                                .then(Mono.error(error))
                                )
                );
    }

    private Mono<Despacho> procesarDatosLogisticos(
            DespachoEntity despachoRecibido,
            CrearDespachoRequest request,
            DatosLogisticos datos
    ) {
        if (datos.scoreRiesgo() > UMBRAL_RIESGO) {
            return actualizarEstado(
                    despachoRecibido.getId(),
                    EstadoDespacho.RECHAZADO,
                    datos
            )
                    .flatMap(this::mapearConPaquetes)
                    .flatMap(eventBus::publicar)
                    .then(Mono.error(new ZonaRiesgosaException(datos.scoreRiesgo())));
        }

        return asignarEnTransaccion(despachoRecibido, request, datos);
    }

    private Mono<DespachoEntity> guardarRecibido(
            CrearDespachoRequest request,
            String idempotencyKey
    ) {
        return Mono.deferContextual(ctx -> {
            DespachoEntity entity = DespachoEntity.builder()
                    .clienteId(request.getClienteId())
                    .ciudad(request.getCiudad())
                    .estado(EstadoDespacho.RECIBIDO)
                    .trazaId(TrazaContext.trazaId(ctx))
                    .idempotencyKey(blankToNull(idempotencyKey))
                    .creadoEn(OffsetDateTime.now())
                    .build();

            log.info(
                    "[{}] Persistiendo despacho RECIBIDO ciudad {}",
                    TrazaContext.trazaId(ctx),
                    request.getCiudad()
            );
            return despachoRepository.save(entity);
        });
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
                                .concatMap(paqueteRepository::save)
                                .then(Mono.just(despacho))
                );

        return transactionalOperator.transactional(asignacion)
                .flatMap(this::mapearConPaquetes);
    }

    private Mono<DespachoEntity> actualizarEstado(
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
                });
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
