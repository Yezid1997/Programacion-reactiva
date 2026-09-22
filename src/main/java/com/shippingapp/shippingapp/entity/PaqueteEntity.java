package com.shippingapp.shippingapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("paquete")
public class PaqueteEntity {

    @Id
    private Long id;

    @Column("despacho_id")
    private Long despachoId;

    @Column("vehiculo_id")
    private Long vehiculoId;

    @Column("peso_kg")
    private Integer pesoKg;
}
