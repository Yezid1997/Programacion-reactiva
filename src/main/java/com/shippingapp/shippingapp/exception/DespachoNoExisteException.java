package com.shippingapp.shippingapp.exception;

public class DespachoNoExisteException extends RuntimeException {

    public DespachoNoExisteException(Long id) {
        super("No existe el despacho " + id);
    }
}
