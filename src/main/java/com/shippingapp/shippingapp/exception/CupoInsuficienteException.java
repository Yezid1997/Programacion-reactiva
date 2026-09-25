package com.shippingapp.shippingapp.exception;

public class CupoInsuficienteException extends RuntimeException {

    public CupoInsuficienteException(Long vehiculoId, Integer pesoKg) {
        super(
                "El vehículo " + vehiculoId
                        + " no tiene cupo suficiente para "
                        + pesoKg + " kg"
        );
    }
}