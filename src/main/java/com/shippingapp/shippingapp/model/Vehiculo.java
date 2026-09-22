package com.shippingapp.shippingapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehiculo {

    private Long id;

    private String placa;

    private String ciudad;

    private Integer cupoKg;

    private Integer reservadoKg;
}
