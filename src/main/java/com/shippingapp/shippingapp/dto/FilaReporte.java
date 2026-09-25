package com.shippingapp.shippingapp.dto;

import java.math.BigDecimal;

public record FilaReporte(
        String ciudad,
        Long despachoId,
        int kilos,
        BigDecimal valor
) {
}
