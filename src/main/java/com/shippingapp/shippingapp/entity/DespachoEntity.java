package com.shippingapp.shippingapp.entity;


import com.shippingapp.shippingapp.model.EstadoDespacho;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("despacho")
public class DespachoEntity {

    @Id
    private Long id;

    @Column("cliente_id")
    private Long clienteId;

    private String ciudad;

    private EstadoDespacho estado;

    private BigDecimal tarifa;

    private BigDecimal total;

    @Column("score_riesgo")
    private Integer scoreRiesgo;

    @Column("traza_id")
    private String trazaId;

    @Column("idem_key")
    private String idempotencyKey;

    @Column("creado_en")
    private OffsetDateTime creadoEn;

    @Column("expira_en")
    private OffsetDateTime expiraEn;
}
