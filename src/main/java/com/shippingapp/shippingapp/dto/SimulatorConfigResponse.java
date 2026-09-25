package com.shippingapp.shippingapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatorConfigResponse {

    private int fallasTarifa;

    private int latenciaClimaMs;

    private int latenciaRiesgoMs;

    private int scoreRiesgo;
}
