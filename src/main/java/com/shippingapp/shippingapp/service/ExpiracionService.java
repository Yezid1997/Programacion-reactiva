package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.repository.DespachoRepository;
import com.shippingapp.shippingapp.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiracionService {

    private final DespachoRepository despachoRepository;
    private final PaqueteRepository paqueteRepository;
    private final CupoService cupoService;

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
        return paqueteRepository.findByDespachoId(despacho.getId())
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
    }
}
