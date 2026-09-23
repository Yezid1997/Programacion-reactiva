package com.shippingapp.shippingapp.exception;

public class VehiculoNoExisteException extends RuntimeException {

    public VehiculoNoExisteException(Long id) {
        super("No existe el vehículo con id " + id);
    }
}