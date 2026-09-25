package com.shippingapp.shippingapp.exception;

public class TarifaNoDisponibleException extends RuntimeException {

    public TarifaNoDisponibleException(String ciudad, Throwable causa) {
        super("Tarifa no disponible para " + ciudad, causa);
    }
}
