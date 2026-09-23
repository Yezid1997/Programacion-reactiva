package com.shippingapp.shippingapp.mapper;

import com.shippingapp.shippingapp.dto.CrearVehiculoRequest;
import com.shippingapp.shippingapp.entity.VehiculoEntity;
import com.shippingapp.shippingapp.model.Vehiculo;
import org.springframework.stereotype.Component;

@Component
public class VehiculoMapper {

    public VehiculoEntity toEntity(CrearVehiculoRequest request) {
        return VehiculoEntity.builder()
                .id(request.getId())
                .placa(request.getPlaca())
                .ciudad(request.getCiudad())
                .cupoKg(request.getCupoKg())
                .reservadoKg(0)
                .build();
    }

    public Vehiculo toModel(VehiculoEntity entity) {
        return Vehiculo.builder()
                .id(entity.getId())
                .placa(entity.getPlaca())
                .ciudad(entity.getCiudad())
                .cupoKg(entity.getCupoKg())
                .reservadoKg(entity.getReservadoKg())
                .build();
    }
}