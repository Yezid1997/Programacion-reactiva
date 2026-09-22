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
@Table("vehiculo")
public class VehiculoEntity {
    @Id
    private Long id;

    private String placa;

    private String ciudad;

    @Column("cupo_kg")
    private Integer cupoKg;

    @Column("reservado_kg")
    private Integer reservadoKg;
}