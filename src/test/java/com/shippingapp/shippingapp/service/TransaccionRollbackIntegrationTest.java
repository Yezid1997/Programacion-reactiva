package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.entity.PaqueteEntity;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.repository.DespachoRepository;
import com.shippingapp.shippingapp.repository.PaqueteRepository;
import com.shippingapp.shippingapp.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIf("com.shippingapp.shippingapp.support.PostgresTestSupport#disponible")
class TransaccionRollbackIntegrationTest {

    @Autowired
    private DespachoRepository despachoRepository;

    @Autowired
    private PaqueteRepository paqueteRepository;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @Test
    void unErrorDentroDeLaTransaccionNoDejaElDespachoPersistido() {
        AtomicLong idGenerado = new AtomicLong();
        DespachoEntity despacho = DespachoEntity.builder()
                .clienteId(99L)
                .ciudad("BOG")
                .estado(EstadoDespacho.RECIBIDO)
                .creadoEn(OffsetDateTime.now())
                .build();

        StepVerifier.create(
                transactionalOperator.transactional(
                        despachoRepository.save(despacho)
                                .flatMap(guardado -> {
                                    idGenerado.set(guardado.getId());
                                    return paqueteRepository.save(PaqueteEntity.builder()
                                                    .despachoId(guardado.getId())
                                                    .vehiculoId(1L)
                                                    .pesoKg(10)
                                                    .build())
                                            .then(Mono.error(new IllegalStateException("forzar rollback")));
                                })
                )
        ).expectError(IllegalStateException.class).verify();

        StepVerifier.create(despachoRepository.findById(idGenerado.get()))
                .verifyComplete();
        StepVerifier.create(paqueteRepository.findByDespachoId(idGenerado.get()))
                .verifyComplete();
    }
}
