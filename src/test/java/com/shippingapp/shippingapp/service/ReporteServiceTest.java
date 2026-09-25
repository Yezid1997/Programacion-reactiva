package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.FilaReporte;
import com.shippingapp.shippingapp.dto.ReporteCiudad;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteServiceTest {

    private final ReporteService reporteService = new ReporteService(null);

    @Test
    void agregaKilosYValorPorCiudadSinContarDosVecesElMismoDespacho() {
        TestPublisher<FilaReporte> publisher = TestPublisher.create();

        StepVerifier.create(
                        reporteService.condensar(reporteService.streamDesde(publisher.flux()))
                )
                .then(() -> publisher.next(
                        new FilaReporte("BOG", 1L, 100, new BigDecimal("15000.00")),
                        new FilaReporte("BOG", 1L, 50, new BigDecimal("15000.00")),
                        new FilaReporte("MDE", 2L, 20, new BigDecimal("8000.00"))
                ).complete())
                .assertNext(totales -> {
                    assertThat(totales).hasSize(2);
                    ReporteCiudad bog = totales.get(0);
                    assertThat(bog.ciudad()).isEqualTo("BOG");
                    assertThat(bog.kilos()).isEqualTo(150);
                    assertThat(bog.valor()).isEqualByComparingTo("15000.00");
                    assertThat(bog.despachos()).isEqualTo(1);
                    ReporteCiudad mde = totales.get(1);
                    assertThat(mde.kilos()).isEqualTo(20);
                    assertThat(mde.valor()).isEqualByComparingTo("8000.00");
                    assertThat(mde.despachos()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void elScanPideFilasConLimitRate() {
        TestPublisher<FilaReporte> publisher = TestPublisher.create();

        reporteService.streamDesde(publisher.flux()).subscribe();

        publisher.assertSubscribers(1);
        publisher.assertMinRequested(ReporteService.LIMITE_DEMANDA);
        publisher.assertMaxRequested(ReporteService.LIMITE_DEMANDA);
    }

    @Test
    void consumidorLentoNoPideLaTablaEntera() {
        TestPublisher<FilaReporte> publisher = TestPublisher.create();
        reporteService.streamDesde(publisher.flux())
                .subscribe(new BaseSubscriber<ReporteCiudad>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }
                });

        publisher.assertMaxRequested(ReporteService.LIMITE_DEMANDA);
    }

    @Test
    void laSumaCorreEnElSchedulerParalelo() {
        StepVerifier.create(reporteService.streamDesde(Flux.just(
                        new FilaReporte("BOG", 1L, 10, new BigDecimal("100.00"))
                )))
                .assertNext(reporte -> {
                    assertThat(reporte.kilos()).isEqualTo(10);
                    assertThat(Thread.currentThread().getName()).contains("parallel");
                })
                .verifyComplete();
    }
}
