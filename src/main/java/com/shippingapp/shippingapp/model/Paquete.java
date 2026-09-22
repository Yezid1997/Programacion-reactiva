package com.shippingapp.shippingapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paquete {

    private Long id;

    private Long despachoId;

    private Long vehiculoId;

    private Integer pesoKg;
}