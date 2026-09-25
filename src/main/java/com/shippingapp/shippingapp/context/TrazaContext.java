package com.shippingapp.shippingapp.context;

import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public final class TrazaContext {

    public static final String TRAZA_ID_KEY = "trazaId";

    private TrazaContext() {
    }

    public static String trazaId(ContextView context) {
        return context.getOrDefault(TRAZA_ID_KEY, "sin-traza");
    }

    /**
     * Lee el trazaId del Reactor Context en el momento del log.
     * No hace falta pasarlo como parámetro por la cadena de métodos.
     */
    public static Mono<Void> info(Logger log, String mensaje, Object... args) {
        return Mono.deferContextual(ctx -> {
            Object[] completos = new Object[args.length + 1];
            completos[0] = trazaId(ctx);
            System.arraycopy(args, 0, completos, 1, args.length);
            log.info("[{}] " + mensaje, completos);
            return Mono.empty();
        });
    }
}
