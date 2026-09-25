package com.shippingapp.shippingapp.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
public final class ReactiveSupport {

    public static final Duration PERIODO_LATIDO = Duration.ofSeconds(15);

    private ReactiveSupport() {
    }

    /**
     * El scan del reporte es CPU (sumar miles de filas). limitRate acota lo que
     * se pide a Postgres; publishOn mueve esa suma al scheduler paralelo para
     * no ocupar el hilo de I/O de R2DBC.
     */
    public static <T> Flux<T> calculoCpu(Flux<T> filas, int limiteDemanda) {
        return filas.limitRate(limiteDemanda)
                .publishOn(Schedulers.parallel());
    }

    public static <T> Flux<ServerSentEvent<T>> latidos(Duration periodo) {
        return Flux.interval(periodo)
                .onBackpressureLatest()
                .map(tick -> ServerSentEvent.<T>builder()
                        .event("heartbeat")
                        .id("hb-" + tick)
                        .comment("ping")
                        .build());
    }

    /**
     * Fusiona los datos con un heartbeat. Cuando los datos terminan (estado
     * terminal) el intervalo se cancela; si el cliente corta, también.
     */
    public static <T> Flux<ServerSentEvent<T>> fundirConHeartbeat(
            Flux<ServerSentEvent<T>> datos,
            Duration periodo
    ) {
        return datos.publish(compartido -> Flux.<ServerSentEvent<T>>merge(
                compartido,
                ReactiveSupport.<T>latidos(periodo).takeUntilOther(compartido.ignoreElements())
        ));
    }

    public static <T> Flux<T> alDesconectar(Flux<T> flujo) {
        return alDesconectar(flujo, signal -> { });
    }

    public static <T> Flux<T> alDesconectar(Flux<T> flujo, Consumer<SignalType> alFinal) {
        return flujo
                .doOnCancel(() -> {
                    log.info("Cliente desconectado; se cancelan heartbeat y suscripción");
                    alFinal.accept(SignalType.CANCEL);
                })
                .doFinally(signal -> {
                    log.info("Stream SSE liberado: {}", signal);
                    if (signal != SignalType.CANCEL) {
                        alFinal.accept(signal);
                    }
                });
    }
}
