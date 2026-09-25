package com.shippingapp.shippingapp.service;

import com.shippingapp.shippingapp.dto.FilaReporte;
import com.shippingapp.shippingapp.dto.ReporteCiudad;
import com.shippingapp.shippingapp.support.ReactiveSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReporteService {

    static final int LIMITE_DEMANDA = 256;

    private final DatabaseClient databaseClient;

    public Mono<List<ReporteCiudad>> totalesPorCiudad() {
        return condensar(streamAcumulado());
    }

    public Flux<ReporteCiudad> streamAcumulado() {
        return streamDesde(leerFilas());
    }

    Flux<ReporteCiudad> streamDesde(Flux<FilaReporte> filas) {
        return Flux.defer(() -> {
            Map<String, Acumulado> acumulados = new LinkedHashMap<>();
            Set<Long> despachosContados = new HashSet<>();
            return ReactiveSupport.calculoCpu(filas, LIMITE_DEMANDA)
                    .handle((fila, sink) -> {
                        if (fila.ciudad() == null || fila.despachoId() == null) {
                            return;
                        }
                        sink.next(aplicar(acumulados, despachosContados, fila));
                    });
        });
    }

    Mono<List<ReporteCiudad>> condensar(Flux<ReporteCiudad> snapshots) {
        return snapshots
                .reduce(new LinkedHashMap<String, ReporteCiudad>(), (mapa, reporte) -> {
                    mapa.put(reporte.ciudad(), reporte);
                    return mapa;
                })
                .map(mapa -> List.copyOf(mapa.values()))
                .defaultIfEmpty(List.of());
    }

    private Flux<FilaReporte> leerFilas() {
        return databaseClient.sql("""
                        SELECT d.ciudad AS ciudad,
                               d.id AS despacho_id,
                               p.peso_kg AS peso_kg,
                               d.total AS total
                        FROM paquete p
                        JOIN despacho d ON d.id = p.despacho_id
                        """)
                .map((row, metadata) -> new FilaReporte(
                        row.get("ciudad", String.class),
                        row.get("despacho_id", Long.class),
                        row.get("peso_kg", Integer.class) == null
                                ? 0
                                : row.get("peso_kg", Integer.class),
                        row.get("total", BigDecimal.class)
                ))
                .all();
    }

    private static ReporteCiudad aplicar(
            Map<String, Acumulado> acumulados,
            Set<Long> despachosContados,
            FilaReporte fila
    ) {
        Acumulado acumulado = acumulados.computeIfAbsent(
                fila.ciudad(),
                Acumulado::new
        );
        acumulado.kilos += fila.kilos();
        if (despachosContados.add(fila.despachoId())) {
            acumulado.despachos++;
            if (fila.valor() != null) {
                acumulado.valor = acumulado.valor.add(fila.valor());
            }
        }
        return acumulado.snapshot();
    }

    private static final class Acumulado {
        private final String ciudad;
        private long kilos;
        private BigDecimal valor = BigDecimal.ZERO;
        private long despachos;

        private Acumulado(String ciudad) {
            this.ciudad = ciudad;
        }

        private ReporteCiudad snapshot() {
            return new ReporteCiudad(ciudad, kilos, valor, despachos);
        }
    }
}
