package com.shippingapp.shippingapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("vehiculo")
public class VehiculoEntity implements Persistable<Long> {

    @Id
    private Long id;

    private String placa;

    private String ciudad;

    @Column("cupo_kg")
    private Integer cupoKg;

    @Column("reservado_kg")
    private Integer reservadoKg;

    /**
     * El id lo asigna el cliente, no BIGSERIAL. Sin esta marca Spring Data
     * interpreta el alta como UPDATE y no inserta la fila.
     */
    @Transient
    @Builder.Default
    @EqualsAndHashCode.Exclude
    private boolean nuevo = true;

    @Override
    public boolean isNew() {
        return nuevo;
    }

    public void marcarPersistido() {
        this.nuevo = false;
    }
}