package com.shippingapp.shippingapp.dto;

import java.time.Instant;

public record ErrorResponse(
        String codigo,
        String mensaje,
        String trazaId,
        Instant instante
) {
}
