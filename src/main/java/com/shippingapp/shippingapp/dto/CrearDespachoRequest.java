package com.shippingapp.shippingapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearDespachoRequest {

    @NotNull(message = "El clienteId es obligatorio")
    private Long clienteId;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(
            max = 8,
            message = "La ciudad no puede superar 8 caracteres"
    )
    private String ciudad;

    @NotEmpty(message = "El despacho debe tener al menos un paquete")
    @Valid
    private List<PaqueteRequest> paquetes;
}