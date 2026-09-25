package com.shippingapp.shippingapp.exception;

public class ZonaRiesgosaException extends RuntimeException {

    public ZonaRiesgosaException(int score) {
        super("Zona de riesgo elevada (score " + score + ", máximo 80)");
    }
}
