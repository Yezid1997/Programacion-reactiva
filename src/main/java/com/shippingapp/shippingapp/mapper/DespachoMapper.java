package com.shippingapp.shippingapp.mapper;

import com.shippingapp.shippingapp.entity.DespachoEntity;
import com.shippingapp.shippingapp.entity.PaqueteEntity;
import com.shippingapp.shippingapp.model.Despacho;
import com.shippingapp.shippingapp.model.Paquete;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DespachoMapper {

    public Despacho toModel(DespachoEntity entity, List<PaqueteEntity> paquetes) {
        return Despacho.builder()
                .id(entity.getId())
                .clienteId(entity.getClienteId())
                .ciudad(entity.getCiudad())
                .estado(entity.getEstado())
                .tarifa(entity.getTarifa())
                .total(entity.getTotal())
                .scoreRiesgo(entity.getScoreRiesgo())
                .trazaId(entity.getTrazaId())
                .idempotencyKey(entity.getIdempotencyKey())
                .creadoEn(entity.getCreadoEn())
                .expiraEn(entity.getExpiraEn())
                .paquetes(paquetes.stream().map(this::toPaqueteModel).toList())
                .build();
    }

    public Paquete toPaqueteModel(PaqueteEntity entity) {
        return Paquete.builder()
                .id(entity.getId())
                .despachoId(entity.getDespachoId())
                .vehiculoId(entity.getVehiculoId())
                .pesoKg(entity.getPesoKg())
                .build();
    }
}
