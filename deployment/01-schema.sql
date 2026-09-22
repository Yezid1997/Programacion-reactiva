DROP TABLE IF EXISTS paquete;
DROP TABLE IF EXISTS despacho;
DROP TABLE IF EXISTS vehiculo;

CREATE TABLE vehiculo (
    id           BIGINT PRIMARY KEY,
    placa        VARCHAR(10)  NOT NULL UNIQUE,
    ciudad       VARCHAR(8)   NOT NULL,
    cupo_kg      INT          NOT NULL CHECK (cupo_kg >= 0),
    reservado_kg INT          NOT NULL DEFAULT 0 CHECK (reservado_kg >= 0)
);

CREATE TABLE despacho (
    id         BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT       NOT NULL,
    ciudad     VARCHAR(8)   NOT NULL,
    estado     VARCHAR(16)  NOT NULL,                             -- RECIBIDO|ASIGNADO|EN_RUTA|ENTREGADO|RECHAZADO|EXPIRADO
    tarifa     NUMERIC(12,2),
    total      NUMERIC(12,2),
    score_riesgo INT,
    traza_id   VARCHAR(64),
    idem_key   VARCHAR(64) UNIQUE,
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expira_en  TIMESTAMPTZ
);

CREATE TABLE paquete (
    id          BIGSERIAL PRIMARY KEY,
    despacho_id BIGINT NOT NULL REFERENCES despacho(id) ON DELETE CASCADE,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculo(id),
    peso_kg     INT    NOT NULL CHECK (peso_kg > 0)
);

CREATE INDEX idx_despacho_expira ON despacho (estado, expira_en);
CREATE INDEX idx_paquete_despacho ON paquete (despacho_id);

INSERT INTO vehiculo (id, placa, ciudad, cupo_kg) VALUES
    (1, 'ABC123', 'BOG', 500),
    (2, 'XYZ987', 'MDE', 200),
    (3, 'JKL456', 'CLO', 800)
ON CONFLICT (id) DO NOTHING;