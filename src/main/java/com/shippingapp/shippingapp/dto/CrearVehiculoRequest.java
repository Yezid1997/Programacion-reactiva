package com.shippingapp.shippingapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearVehiculoRequest {

    @NotNull(message = "El id es obligatorio")
    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 10)
    private String placa;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 8)
    private String ciudad;

    @NotNull(message = "El cupoKg es obligatorio")
    @Min(value = 0)
    private Integer cupoKg;
}