package com.shippingapp.shippingapp.exception;

public class RiesgoNoDisponibleException extends RuntimeException {

    public RiesgoNoDisponibleException(String ciudad, Throwable causa) {
        super("Score de riesgo no disponible para " + ciudad, causa);
    }
}
