package com.shippingapp.shippingapp.exception;

import com.shippingapp.shippingapp.model.EstadoDespacho;

public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(Long despachoId, EstadoDespacho estadoActual) {
        super(
                "El despacho " + despachoId
                        + " no permite la operación en estado "
                        + estadoActual
        );
    }
}
