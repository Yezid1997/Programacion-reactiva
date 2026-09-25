package com.shippingapp.shippingapp.dto;

import java.math.BigDecimal;

public record ReporteCiudad(
        String ciudad,
        long kilos,
        BigDecimal valor,
        long despachos
) {
}
