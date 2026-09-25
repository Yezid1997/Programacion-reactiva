package com.shippingapp.shippingapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatorConfigRequest {

    private Integer fallasTarifa;

    private Integer latenciaClimaMs;

    private Integer latenciaRiesgoMs;

    private Integer scoreRiesgo;
}
