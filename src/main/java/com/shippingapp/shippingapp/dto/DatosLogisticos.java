package com.shippingapp.shippingapp.dto;

import java.math.BigDecimal;

public record DatosLogisticos(
        BigDecimal tarifa,
        int ventanaHoras,
        String condicionClima,
        int scoreRiesgo
) {
}
