package com.shippingapp.shippingapp.external;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimulatorState {

    private final AtomicInteger fallasTarifa = new AtomicInteger(0);
    private final AtomicInteger latenciaClimaMs = new AtomicInteger(400);
    private final AtomicInteger latenciaRiesgoMs = new AtomicInteger(0);
    private final AtomicInteger scoreRiesgo = new AtomicInteger(20);

    public int getFallasTarifa() {
        return fallasTarifa.get();
    }

    public int getLatenciaClimaMs() {
        return latenciaClimaMs.get();
    }

    public int getLatenciaRiesgoMs() {
        return latenciaRiesgoMs.get();
    }

    public int getScoreRiesgo() {
        return scoreRiesgo.get();
    }

    public void actualizar(
            Integer fallasTarifa,
            Integer latenciaClimaMs,
            Integer latenciaRiesgoMs,
            Integer scoreRiesgo
    ) {
        if (fallasTarifa != null) {
            this.fallasTarifa.set(Math.max(0, fallasTarifa));
        }
        if (latenciaClimaMs != null) {
            this.latenciaClimaMs.set(Math.max(0, latenciaClimaMs));
        }
        if (latenciaRiesgoMs != null) {
            this.latenciaRiesgoMs.set(Math.max(0, latenciaRiesgoMs));
        }
        if (scoreRiesgo != null) {
            this.scoreRiesgo.set(scoreRiesgo);
        }
    }

    public void reset() {
        fallasTarifa.set(0);
        latenciaClimaMs.set(400);
        latenciaRiesgoMs.set(0);
        scoreRiesgo.set(20);
    }

    /**
     * @return true si debe simularse un fallo transitorio en tarifa
     */
    public boolean consumirFallaTarifa() {
        for (; ; ) {
            int actual = fallasTarifa.get();
            if (actual <= 0) {
                return false;
            }
            if (fallasTarifa.compareAndSet(actual, actual - 1)) {
                return true;
            }
        }
    }
}
