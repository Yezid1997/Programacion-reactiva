package com.shippingapp.shippingapp.exception;

import com.shippingapp.shippingapp.context.TrazaContext;
import com.shippingapp.shippingapp.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(VehiculoNoExisteException.class)
    public Mono<ResponseEntity<ErrorResponse>> vehiculoNoExiste(
            VehiculoNoExisteException ex
    ) {
        return respuesta("VEHICULO_NO_EXISTE", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CupoInsuficienteException.class)
    public Mono<ResponseEntity<ErrorResponse>> cupoInsuficiente(
            CupoInsuficienteException ex
    ) {
        return respuesta("CUPO_INSUFICIENTE", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ZonaRiesgosaException.class)
    public Mono<ResponseEntity<ErrorResponse>> zonaRiesgosa(ZonaRiesgosaException ex) {
        return respuesta("ZONA_RIESGOSA", ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(DespachoNoExisteException.class)
    public Mono<ResponseEntity<ErrorResponse>> despachoNoExiste(
            DespachoNoExisteException ex
    ) {
        return respuesta("DESPACHO_NO_EXISTE", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public Mono<ResponseEntity<ErrorResponse>> estadoInvalido(
            EstadoInvalidoException ex
    ) {
        return respuesta("ESTADO_INVALIDO", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> validacion(WebExchangeBindException ex) {
        String mensaje = ex.getAllErrors().isEmpty()
                ? "Petición inválida"
                : ex.getAllErrors().get(0).getDefaultMessage();
        return respuesta("VALIDACION", mensaje, HttpStatus.BAD_REQUEST);
    }

    private Mono<ResponseEntity<ErrorResponse>> respuesta(
            String codigo,
            String mensaje,
            HttpStatus status
    ) {
        return Mono.deferContextual(ctx -> Mono.just(
                ResponseEntity.status(status).body(
                        new ErrorResponse(
                                codigo,
                                mensaje,
                                TrazaContext.trazaId(ctx),
                                Instant.now()
                        )
                )
        ));
    }
}
