package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.entity.VehiculoEntity;
import com.shippingapp.shippingapp.exception.CupoInsuficienteException;
import com.shippingapp.shippingapp.exception.VehiculoNoExisteException;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CupoService {

    private final DatabaseClient databaseClient;

    public Mono<VehiculoEntity> reservar(Long vehiculoId, Integer pesoKg) {

        return databaseClient.sql("""
                UPDATE vehiculo
                SET cupo_kg = cupo_kg - :peso,
                    reservado_kg = reservado_kg + :peso
                WHERE id = :id
                  AND cupo_kg >= :peso
                RETURNING id, placa, ciudad, cupo_kg, reservado_kg
                """)
                .bind("peso", pesoKg)
                .bind("id", vehiculoId)
                .map((row, metadata) ->
                        VehiculoEntity.builder()
                                .id(row.get("id", Long.class))
                                .placa(row.get("placa", String.class))
                                .ciudad(row.get("ciudad", String.class))
                                .cupoKg(row.get("cupo_kg", Integer.class))
                                .reservadoKg(row.get(
                                        "reservado_kg",
                                        Integer.class
                                ))
                                .build()
                )
                .one()
                .switchIfEmpty(
                        determinarErrorReserva(vehiculoId, pesoKg)
                );
    }

    public Mono<VehiculoEntity> liberar(Long vehiculoId, Integer pesoKg) {
        return databaseClient.sql("""
                UPDATE vehiculo
                SET cupo_kg = cupo_kg + :peso,
                    reservado_kg = reservado_kg - :peso
                WHERE id = :id
                  AND reservado_kg >= :peso
                RETURNING id, placa, ciudad, cupo_kg, reservado_kg
                """)
                .bind("peso", pesoKg)
                .bind("id", vehiculoId)
                .map((row, metadata) ->
                        VehiculoEntity.builder()
                                .id(row.get("id", Long.class))
                                .placa(row.get("placa", String.class))
                                .ciudad(row.get("ciudad", String.class))
                                .cupoKg(row.get("cupo_kg", Integer.class))
                                .reservadoKg(row.get("reservado_kg", Integer.class))
                                .build()
                )
                .one()
                .switchIfEmpty(
                        Mono.error(
                                new VehiculoNoExisteException(vehiculoId)
                        )
                );
    }

    public Mono<VehiculoEntity> consumirReserva(Long vehiculoId, Integer pesoKg) {
        return databaseClient.sql("""
                UPDATE vehiculo
                SET reservado_kg = reservado_kg - :peso
                WHERE id = :id
                  AND reservado_kg >= :peso
                RETURNING id, placa, ciudad, cupo_kg, reservado_kg
                """)
                .bind("peso", pesoKg)
                .bind("id", vehiculoId)
                .map((row, metadata) ->
                        VehiculoEntity.builder()
                                .id(row.get("id", Long.class))
                                .placa(row.get("placa", String.class))
                                .ciudad(row.get("ciudad", String.class))
                                .cupoKg(row.get("cupo_kg", Integer.class))
                                .reservadoKg(row.get("reservado_kg", Integer.class))
                                .build()
                )
                .one()
                .switchIfEmpty(
                        Mono.error(
                                new VehiculoNoExisteException(vehiculoId)
                        )
                );
    }

    private Mono<VehiculoEntity> determinarErrorReserva(
            Long vehiculoId,
            Integer pesoKg
    ) {

        return databaseClient.sql("""
                SELECT id
                FROM vehiculo
                WHERE id = :id
                """)
                .bind("id", vehiculoId)
                .map((row, metadata) -> row.get("id", Long.class))
                .one()
                .flatMap(id ->
                        Mono.<VehiculoEntity>error(
                                new CupoInsuficienteException(
                                        vehiculoId,
                                        pesoKg
                                )
                        )
                )
                .switchIfEmpty(
                        Mono.error(
                                new VehiculoNoExisteException(vehiculoId)
                        )
                );
    }
}
