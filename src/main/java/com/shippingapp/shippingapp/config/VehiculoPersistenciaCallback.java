package com.shippingapp.shippingapp.config;

import com.shippingapp.shippingapp.entity.VehiculoEntity;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class VehiculoPersistenciaCallback
        implements AfterConvertCallback<VehiculoEntity>, AfterSaveCallback<VehiculoEntity> {

    @Override
    public Publisher<VehiculoEntity> onAfterConvert(VehiculoEntity entity, SqlIdentifier table) {
        entity.marcarPersistido();
        return Mono.just(entity);
    }

    @Override
    public Publisher<VehiculoEntity> onAfterSave(
            VehiculoEntity entity,
            OutboundRow outboundRow,
            SqlIdentifier table
    ) {
        entity.marcarPersistido();
        return Mono.just(entity);
    }
}
