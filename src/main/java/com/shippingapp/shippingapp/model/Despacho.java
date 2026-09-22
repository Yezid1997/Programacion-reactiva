package com.shippingapp.shippingapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Despacho {

    private Long id;

    private Long clienteId;

    private String ciudad;

    private EstadoDespacho estado;

    private BigDecimal tarifa;

    private BigDecimal total;

    private Integer scoreRiesgo;

    private String trazaId;

    private String idempotencyKey;

    private OffsetDateTime creadoEn;

    private OffsetDateTime expiraEn;

    private List<Paquete> paquetes;
}