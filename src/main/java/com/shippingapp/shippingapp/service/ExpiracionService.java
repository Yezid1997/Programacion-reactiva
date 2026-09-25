package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.mapper.DespachoMapper;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.repository.DespachoRepository;
import com.shippingapp.shippingapp.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiracionService {

    private final DespachoRepository despachoRepository;
    private final PaqueteRepository paqueteRepository;
    private final CupoService cupoService;
    private final DespachoMapper despachoMapper;
    private final DespachoEventBus eventBus;
    private final TransactionalOperator transactionalOperator;

    public Mono<Long> expirarAsignacionesVencidas() {
        OffsetDateTime ahora = OffsetDateTime.now();
        return despachoRepository.findByEstadoAndExpiraEnBefore(
                        EstadoDespacho.ASIGNADO,
                        ahora
                )
                .concatMap(this::expirarDespacho)
                .count()
                .doOnNext(cantidad -> {
                    if (cantidad > 0) {
                        log.info("Expiraron {} asignaciones de despacho", cantidad);
                    }
                });
    }

    private Mono<DespachoEntity> expirarDespacho(DespachoEntity despacho) {
        Mono<DespachoEntity> operacion = paqueteRepository.findByDespachoId(despacho.getId())
                .concatMap(paquete ->
                        cupoService.liberar(
                                paquete.getVehiculoId(),
                                paquete.getPesoKg()
                        )
                )
                .then(Mono.defer(() -> {
                    despacho.setEstado(EstadoDespacho.EXPIRADO);
                    despacho.setExpiraEn(null);
                    return despachoRepository.save(despacho);
                }));

        return transactionalOperator.transactional(operacion)
                .flatMap(guardado -> paqueteRepository.findByDespachoId(guardado.getId())
                        .collectList()
                        .map(paquetes -> despachoMapper.toModel(guardado, paquetes))
                        .flatMap(eventBus::publicar)
                        .thenReturn(guardado));
    }
}
