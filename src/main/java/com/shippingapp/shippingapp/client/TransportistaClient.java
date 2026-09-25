package com.shippingapp.shippingapp.client;

import com.shippingapp.shippingapp.context.TrazaContext;
import com.shippingapp.shippingapp.dto.ClimaResponse;
import com.shippingapp.shippingapp.dto.DatosLogisticos;
import com.shippingapp.shippingapp.dto.RiesgoResponse;
import com.shippingapp.shippingapp.dto.TarifaResponse;
import com.shippingapp.shippingapp.service.TarifaCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransportistaClient {

    private static final Duration TIMEOUT_RIESGO = Duration.ofMillis(800);
    private static final Duration CACHE_CLIMA = Duration.ofMinutes(10);
    private static final int SCORE_RIESGO_DEFAULT = 50;

    private final WebClient webClient;
    private final TarifaCatalogService tarifaCatalogService;

    private final Map<String, Mono<ClimaResponse>> climaPorCiudad = new ConcurrentHashMap<>();

    public Mono<DatosLogisticos> consultarDatosLogisticos(String ciudad) {
        return TrazaContext.info(log, "Consultando externos en paralelo para {}", ciudad)
                .then(Mono.zip(
                        consultarTarifa(ciudad),
                        consultarClima(ciudad),
                        consultarRiesgo(ciudad)
                ))
                .map(tuple -> new DatosLogisticos(
                        tuple.getT1().tarifa(),
                        tuple.getT2().ventanaHoras(),
                        tuple.getT2().condicion(),
                        tuple.getT3().score()
                ));
    }

    public Mono<TarifaResponse> consultarTarifa(String ciudad) {
        return webClient.get()
                .uri("/external/tarifas/{ciudad}", ciudad)
                .retrieve()
                .bodyToMono(TarifaResponse.class)
                .retryWhen(reintentosTarifa())
                .onErrorResume(error -> tarifaCatalogService.tarifaBase(ciudad)
                        .map(base -> new TarifaResponse(ciudad, base)));
    }

    public Mono<ClimaResponse> consultarClima(String ciudad) {
        return climaPorCiudad.computeIfAbsent(ciudad, this::climaCacheado);
    }

    public Mono<RiesgoResponse> consultarRiesgo(String ciudad) {
        return webClient.get()
                .uri("/external/riesgo/{ciudad}", ciudad)
                .retrieve()
                .bodyToMono(RiesgoResponse.class)
                .timeout(TIMEOUT_RIESGO)
                .onErrorReturn(new RiesgoResponse(ciudad, SCORE_RIESGO_DEFAULT));
    }

    static Retry reintentosTarifa() {
        return Retry.backoff(3, Duration.ofMillis(200))
                .maxBackoff(Duration.ofSeconds(2))
                .jitter(0.2)
                .filter(TransportistaClient::esErrorTransitorio);
    }

    static boolean esErrorTransitorio(Throwable error) {
        if (error instanceof WebClientResponseException response) {
            return response.getStatusCode().is5xxServerError()
                    || response.getStatusCode().value() == 429;
        }
        return error instanceof IOException;
    }

    private Mono<ClimaResponse> climaCacheado(String ciudad) {
        return webClient.get()
                .uri("/external/clima/{ciudad}", ciudad)
                .retrieve()
                .bodyToMono(ClimaResponse.class)
                .cache(CACHE_CLIMA);
    }
}
