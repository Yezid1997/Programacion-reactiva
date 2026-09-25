package com.shippingapp.shippingapp.client;

import com.shippingapp.shippingapp.service.TarifaCatalogService;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TransportistaClientTest {

    private MockWebServer mockWebServer;
    private TransportistaClient transportistaClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        transportistaClient = new TransportistaClient(
                webClient,
                new TarifaCatalogService()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void usaTarifaDeCatalogoCuandoFallaElServicioExterno() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        StepVerifier.create(transportistaClient.consultarTarifa("BOG"))
                .assertNext(tarifa -> {
                    assertThat(tarifa.ciudad()).isEqualTo("BOG");
                    assertThat(tarifa.tarifa()).isEqualByComparingTo("15000.00");
                })
                .verifyComplete();
    }

    @Test
    void consultaExternosEnParaleloConMonoZip() {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/tarifas/")) {
                    return json("{\"ciudad\":\"BOG\",\"tarifa\":18500.00}");
                }
                if (path != null && path.contains("/clima/")) {
                    return json("{\"ciudad\":\"BOG\",\"ventanaHoras\":6,\"condicion\":\"SOLEADO\"}");
                }
                if (path != null && path.contains("/riesgo/")) {
                    return json("{\"ciudad\":\"BOG\",\"score\":25}");
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        StepVerifier.create(transportistaClient.consultarDatosLogisticos("BOG"))
                .assertNext(datos -> {
                    assertThat(datos.tarifa()).isEqualByComparingTo("18500.00");
                    assertThat(datos.ventanaHoras()).isEqualTo(6);
                    assertThat(datos.scoreRiesgo()).isEqualTo(25);
                })
                .verifyComplete();
    }

    @Test
    void cacheaClimaPorCiudad() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ciudad\":\"MDE\",\"ventanaHoras\":4,\"condicion\":\"LLUVIA\"}")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(transportistaClient.consultarClima("MDE"))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(transportistaClient.consultarClima("MDE"))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void timeoutEnRiesgoDevuelveScorePorDefecto() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ciudad\":\"BOG\",\"score\":99}")
                .addHeader("Content-Type", "application/json")
                .setBodyDelay(2, java.util.concurrent.TimeUnit.SECONDS));

        StepVerifier.create(transportistaClient.consultarRiesgo("BOG"))
                .assertNext(riesgo -> {
                    assertThat(riesgo.score()).isEqualTo(50);
                    assertThat(riesgo.ciudad()).isEqualTo("BOG");
                })
                .verifyComplete();
    }

    @Test
    void backoffTarifaReintentaSoloErroresTransitorios() {
        AtomicInteger intentos = new AtomicInteger();

        Mono<String> pipeline = Mono.defer(() -> {
                    if (intentos.incrementAndGet() <= 3) {
                        return Mono.error(new WebClientResponseException(
                                503,
                                "Service Unavailable",
                                null,
                                null,
                                null
                        ));
                    }
                    return Mono.just("ok");
                })
                .retryWhen(TransportistaClient.reintentosTarifa());

        StepVerifier.withVirtualTime(() -> pipeline)
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(5))
                .expectNext("ok")
                .verifyComplete();

        assertThat(intentos.get()).isEqualTo(4);
    }

    @Test
    void noReintentaErrores4xx() {
        AtomicInteger intentos = new AtomicInteger();

        Mono<String> pipeline = Mono.<String>defer(() -> Mono.error(new WebClientResponseException(
                        422,
                        "Unprocessable",
                        null,
                        null,
                        null
                )))
                .doOnSubscribe(s -> intentos.incrementAndGet())
                .retryWhen(TransportistaClient.reintentosTarifa());

        StepVerifier.create(pipeline)
                .expectError(WebClientResponseException.class)
                .verify();

        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void error4xxNoReintentaYCaeAlCatalogo() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        StepVerifier.create(transportistaClient.consultarTarifa("BOG"))
                .assertNext(tarifa ->
                        assertThat(tarifa.tarifa()).isEqualByComparingTo("15000.00"))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void zipTardaLoDeLaLlamadaMasLenta() {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                MockResponse respuesta;
                if (path != null && path.contains("/tarifas/")) {
                    respuesta = json("{\"ciudad\":\"BOG\",\"tarifa\":18500.00}");
                } else if (path != null && path.contains("/clima/")) {
                    respuesta = json("{\"ciudad\":\"BOG\",\"ventanaHoras\":6,\"condicion\":\"SOLEADO\"}");
                } else if (path != null && path.contains("/riesgo/")) {
                    respuesta = json("{\"ciudad\":\"BOG\",\"score\":25}");
                } else {
                    respuesta = new MockResponse().setResponseCode(404);
                }
                return respuesta.setBodyDelay(350, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        });

        long inicio = System.nanoTime();
        StepVerifier.create(transportistaClient.consultarDatosLogisticos("BOG"))
                .expectNextCount(1)
                .verifyComplete();
        long transcurridoMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - inicio
        );

        assertThat(transcurridoMs).isLessThan(900);
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json");
    }
}
