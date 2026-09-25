package com.shippingapp.shippingapp.controller;

import com.shippingapp.shippingapp.dto.CargaMasivaResponse;
import com.shippingapp.shippingapp.dto.ReporteCiudad;
import com.shippingapp.shippingapp.exception.EstadoInvalidoException;
import com.shippingapp.shippingapp.exception.GlobalErrorHandler;
import com.shippingapp.shippingapp.exception.ZonaRiesgosaException;
import com.shippingapp.shippingapp.filter.TrazaWebFilter;
import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.model.EstadoDespacho;
import com.shippingapp.shippingapp.model.Vehiculo;
import com.shippingapp.shippingapp.service.DespachoEventBus;
import com.shippingapp.shippingapp.service.DespachoService;
import com.shippingapp.shippingapp.service.ReporteService;
import com.shippingapp.shippingapp.service.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {
        ShippingController.class,
        VehicleController.class,
        OpsController.class,
        ReportController.class
})
@Import({TrazaWebFilter.class, DespachoEventBus.class, GlobalErrorHandler.class})
class ApiWebTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DespachoEventBus eventBus;

    @MockitoBean
    private DespachoService despachoService;

    @MockitoBean
    private VehiculoService vehiculoService;

    @MockitoBean
    private ReporteService reporteService;

    @Test
    void validacionRechazaElCuerpoYPropagaElTrazaId() {
        webTestClient.post()
                .uri("/api/despachos")
                .header("X-Traza-Id", "traza-web")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"ciudad\":\"BOG\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Traza-Id", "traza-web")
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.trazaId").isEqualTo("traza-web");
    }

    @Test
    void zonaRiesgosaResponde422() {
        when(despachoService.crear(any(), any()))
                .thenReturn(Mono.error(new ZonaRiesgosaException(95)));

        webTestClient.post()
                .uri("/api/despachos")
                .header("X-Traza-Id", "riesgo-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"clienteId":1,"ciudad":"BOG","paquetes":[{"vehiculoId":1,"pesoKg":10}]}
                        """)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("ZONA_RIESGOSA")
                .jsonPath("$.trazaId").isEqualTo("riesgo-1");
    }

    @Test
    void confirmacionEnEstadoInvalidoResponde409() {
        when(despachoService.confirmar(3L))
                .thenReturn(Mono.error(new EstadoInvalidoException(3L, EstadoDespacho.EN_RUTA)));

        webTestClient.post()
                .uri("/api/despachos/3/confirm")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("ESTADO_INVALIDO");
    }

    @Test
    void eventsEntregaElEstadoYCierraEnTerminal() {
        when(despachoService.buscarPorId(5L)).thenReturn(Mono.just(
                Despacho.builder()
                        .id(5L)
                        .estado(EstadoDespacho.EN_RUTA)
                        .ciudad("BOG")
                        .build()
        ));

        Flux<String> cuerpo = webTestClient.get()
                .uri("/api/despachos/5/events")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(String.class)
                .getResponseBody();

        StepVerifier.create(cuerpo)
                .expectNextMatches(evento -> evento.contains("EN_RUTA"))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void tableroCalienteRecibeElEventoDelBus() {
        OpsController controller = new OpsController(eventBus);

        StepVerifier.create(controller.getDashBoard())
                .then(() -> eventBus.publicar(
                        Despacho.builder()
                                .id(9L)
                                .estado(EstadoDespacho.ASIGNADO)
                                .ciudad("MDE")
                                .build()
                ).block(Duration.ofSeconds(2)))
                .assertNext(evento -> {
                    assertThat(evento.event()).isEqualTo("ASIGNADO");
                    assertThat(evento.data()).isNotNull();
                    assertThat(evento.data().getCiudad()).isEqualTo("MDE");
                })
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void cargaNdjsonYRespondeResumen() {
        when(vehiculoService.cargarMasivo(any())).thenAnswer(invocation -> {
            Flux<Vehiculo> vehiculos = invocation.getArgument(0);
            return vehiculos.count()
                    .map(total -> new CargaMasivaResponse(total, total == 0 ? 0 : 1));
        });

        webTestClient.post()
                .uri("/api/vehiculos/bulk")
                .contentType(MediaType.APPLICATION_NDJSON)
                .bodyValue("""
                        {"id":10,"placa":"AAA111","ciudad":"BOG","cupoKg":100}
                        {"id":11,"placa":"BBB222","ciudad":"MDE","cupoKg":80}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.procesados").isEqualTo(2)
                .jsonPath("$.lotes").isEqualTo(1);
    }

    @Test
    void reportePorCiudadRespondeJson() {
        when(reporteService.totalesPorCiudad()).thenReturn(Mono.just(List.of(
                new ReporteCiudad("BOG", 150, new BigDecimal("15000.00"), 1)
        )));

        webTestClient.get()
                .uri("/api/reports/ciudades")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].ciudad").isEqualTo("BOG")
                .jsonPath("$[0].kilos").isEqualTo(150)
                .jsonPath("$[0].despachos").isEqualTo(1);
    }

    @Test
    void reporteStreamSaleEnNdjson() {
        when(reporteService.streamAcumulado()).thenReturn(Flux.just(
                new ReporteCiudad("CLO", 20, new BigDecimal("8000.00"), 1)
        ));

        webTestClient.get()
                .uri("/api/reports/ciudades/stream")
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
                .expectBody(String.class)
                .value(cuerpo -> org.assertj.core.api.Assertions.assertThat(cuerpo).contains("CLO"));
    }
}
