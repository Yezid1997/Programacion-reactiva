package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.CargaMasivaResponse;
import com.shippingapp.shippingapp.dto.CrearVehiculoRequest;
import com.shippingapp.shippingapp.exception.VehiculoNoExisteException;
import com.shippingapp.shippingapp.mapper.VehiculoMapper;
import com.shippingapp.shippingapp.model.Vehiculo;
import com.shippingapp.shippingapp.repository.VehiculoRepository;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    static final int TAMANO_LOTE = 500;

    private static final String UPSERT = """
            INSERT INTO vehiculo (id, placa, ciudad, cupo_kg, reservado_kg)
            VALUES ($1, $2, $3, $4, 0)
            ON CONFLICT (id) DO UPDATE SET
                placa = EXCLUDED.placa,
                ciudad = EXCLUDED.ciudad,
                cupo_kg = EXCLUDED.cupo_kg
            """;

    private final VehiculoRepository vehiculoRepository;
    private final VehiculoMapper vehiculoMapper;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;

    public Flux<Vehiculo> listar() {
        return vehiculoRepository.findAll()
                .map(vehiculoMapper::toModel);
    }

    public Mono<Vehiculo> buscarPorId(Long id) {
        return vehiculoRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new VehiculoNoExisteException(id)
                ))
                .map(vehiculoMapper::toModel);
    }

    public Mono<Vehiculo> crear(CrearVehiculoRequest request) {
        return Mono.defer(() ->
                vehiculoRepository.save(
                        vehiculoMapper.toEntity(request)
                )
        ).map(vehiculoMapper::toModel);
    }

    public Mono<Void> eliminar(Long id) {
        return vehiculoRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new VehiculoNoExisteException(id)
                ))
                .flatMap(vehiculoRepository::delete);
    }

    public Mono<CargaMasivaResponse> cargarMasivo(Flux<Vehiculo> vehiculos) {
        return vehiculos
                .buffer(TAMANO_LOTE)
                .concatMap(this::upsertLote)
                .reduce(new long[]{0L, 0L}, (acumulado, procesados) -> {
                    acumulado[0] += procesados;
                    acumulado[1] += 1;
                    return acumulado;
                })
                .map(acumulado -> new CargaMasivaResponse(acumulado[0], (int) acumulado[1]))
                .defaultIfEmpty(new CargaMasivaResponse(0, 0));
    }

    private Mono<Long> upsertLote(List<Vehiculo> lote) {
        if (lote.isEmpty()) {
            return Mono.just(0L);
        }
        for (Vehiculo vehiculo : lote) {
            validarFila(vehiculo);
        }
        Mono<Long> escritura = databaseClient.inConnection(connection -> {
            Statement statement = connection.createStatement(UPSERT);
            for (int i = 0; i < lote.size(); i++) {
                Vehiculo vehiculo = lote.get(i);
                if (i > 0) {
                    statement.add();
                }
                statement.bind(0, vehiculo.getId())
                        .bind(1, vehiculo.getPlaca())
                        .bind(2, vehiculo.getCiudad())
                        .bind(3, vehiculo.getCupoKg());
            }
            return Flux.from(statement.execute())
                    .concatMap(Result::getRowsUpdated)
                    .reduce(0L, Long::sum);
        });
        return transactionalOperator.transactional(escritura);
    }

    private static void validarFila(Vehiculo vehiculo) {
        if (vehiculo.getId() == null
                || vehiculo.getPlaca() == null
                || vehiculo.getPlaca().isBlank()
                || vehiculo.getCiudad() == null
                || vehiculo.getCiudad().isBlank()
                || vehiculo.getCupoKg() == null
                || vehiculo.getCupoKg() < 0) {
            throw new IllegalArgumentException(
                    "Cada línea NDJSON requiere id, placa, ciudad y cupoKg >= 0"
            );
        }
    }
}
