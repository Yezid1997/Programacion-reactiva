package com.shippingapp.shippingapp.context;

import reactor.util.context.ContextView;

public final class TrazaContext {

    public static final String TRAZA_ID_KEY = "trazaId";

    private TrazaContext() {
    }

    public static String trazaId(ContextView context) {
        return context.getOrDefault(TRAZA_ID_KEY, "sin-traza");
    }
}
