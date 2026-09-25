# Despacho reactivo de envíos

Servicio **Spring Boot 4 · WebFlux · R2DBC · PostgreSQL 15** para el taller de programación reactiva (dominio: despacho logístico).

---

## Requisitos

| Herramienta | Versión / nota |
|-------------|----------------|
| Java | 17 |
| Docker Desktop | Postgres vía Compose |
| PowerShell | 7+ (usar `curl.exe`, no el alias `curl`) |

---

## Cómo levantar

```powershell
cd deployment
docker compose up -d
cd ..
.\gradlew.bat build
.\gradlew.bat bootRun
```

La API queda en **http://localhost:8081**. Base de datos: `shippingapp` en el puerto **5432**.

Para resetear datos:

```powershell
cd deployment
docker compose down -v
docker compose up -d
```

---

## Headers habituales

| Header | Uso |
|--------|-----|
| `Content-Type: application/json` | Cuerpos JSON |
| `X-Traza-Id` | Trazabilidad; si falta, la app genera uno y lo devuelve en la respuesta |
| `Idempotency-Key` | Solo en `POST /api/despachos`; repeticiones devuelven el mismo despacho (HTTP 200) |

---

## Endpoints

### Despachos — `/api/despachos`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `POST` | `/api/despachos` | Crea despacho: RECIBIDO → reserva cupo → externos en paralelo → ASIGNADO o 422 si riesgo > 80 | JSON `Despacho` (**201** nuevo, **200** idempotente) |
| `GET` | `/api/despachos/{id}` | Despacho con lista de paquetes | JSON |
| `POST` | `/api/despachos/{id}/confirm` | ASIGNADO → EN_RUTA; consume cupo reservado | JSON |
| `GET` | `/api/despachos/{id}/events` | SSE del despacho (cierra en estado terminal) | `text/event-stream` |

**Ejemplo crear despacho** (cuerpo en archivo en PowerShell):

```powershell
@'
{"clienteId":1,"ciudad":"BOG","paquetes":[{"vehiculoId":1,"pesoKg":120}]}
'@ | Set-Content -Encoding utf8NoBOM despacho.json

curl.exe -i -X POST http://localhost:8081/api/despachos `
  -H "Content-Type: application/json" `
  -H "X-Traza-Id: demo-1" `
  -H "Idempotency-Key: K1" `
  --data-binary "@despacho.json"
```

---

### Vehículos — `/api/vehiculos`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/vehiculos` | Lista vehículos | JSON array |
| `GET` | `/api/vehiculos/{id}` | Detalle | JSON |
| `POST` | `/api/vehiculos` | Alta | JSON (**201**) |
| `DELETE` | `/api/vehiculos/{id}` | Baja | **204** |
| `POST` | `/api/vehiculos/bulk` | Carga NDJSON en lotes de 500 (`ON CONFLICT (id) DO UPDATE`) | JSON `{procesados, lotes}` |

---

### Operaciones — `/api/ops`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/ops/tablero` | SSE global compartido (hot, mismo `Sinks.multicast`) | `text/event-stream` |

---

### Reportes — `/api/reports`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| `GET` | `/api/reports/ciudades` | Kilos, valor y despachos por ciudad | JSON array |
| `GET` | `/api/reports/ciudades/stream` | Acumulado en vivo; el scan pide filas con `limitRate(256)` | `application/x-ndjson` |

---

### Servicios externos simulados — `/external`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/external/tarifas/{ciudad}` | Tarifa; fallos 503 configurables |
| `GET` | `/external/clima/{ciudad}` | Ventana/clima (respuesta lenta simulada) |
| `GET` | `/external/riesgo/{ciudad}` | Score de riesgo (latencia configurable) |
| `GET` | `/external/simulator` | Estado actual del simulador |
| `PUT` | `/external/simulator` | Ajusta fallas, latencias y score |
| `DELETE` | `/external/simulator` | Valores por defecto |

**Ejemplo romper externos (demo):**

```powershell
@'
{"fallasTarifa":2,"latenciaRiesgoMs":3000,"scoreRiesgo":95}
'@ | Set-Content -Encoding utf8NoBOM sim.json

curl.exe -X PUT http://localhost:8081/external/simulator `
  -H "Content-Type: application/json" --data-binary "@sim.json"
```

**Carga masiva NDJSON** (lotes de 500, upsert por `id`):

```powershell
@'
{"id":10,"placa":"AAA111","ciudad":"BOG","cupoKg":400}
{"id":11,"placa":"BBB222","ciudad":"MDE","cupoKg":250}
'@ | Set-Content -Encoding utf8NoBOM vehiculos.ndjson

curl.exe -i -X POST http://localhost:8081/api/vehiculos/bulk `
  -H "Content-Type: application/x-ndjson" `
  --data-binary "@vehiculos.ndjson"
```

**SSE** (el despacho cierra en estado terminal; el tablero comparte el mismo bus):

```powershell
curl.exe -N http://localhost:8081/api/despachos/1/events -H "Accept: text/event-stream"
curl.exe -N http://localhost:8081/api/ops/tablero -H "Accept: text/event-stream"
```

**Reporte** (kilos y valor por ciudad; el stream es el acumulado del scan):

```powershell
curl.exe http://localhost:8081/api/reports/ciudades
curl.exe -N http://localhost:8081/api/reports/ciudades/stream -H "Accept: application/x-ndjson"
```

---

### Salud

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/actuator/health` | Health check |

---

## Errores de dominio

Cuerpo uniforme:

```json
{
  "codigo": "CUPO_INSUFICIENTE",
  "mensaje": "...",
  "trazaId": "demo-1",
  "instante": "2026-09-25T12:00:00Z"
}
```

| HTTP | Código | Cuándo |
|------|--------|--------|
| 400 | `VALIDACION` | `@Valid` falla |
| 404 | `VEHICULO_NO_EXISTE` | Vehículo inexistente |
| 404 | `DESPACHO_NO_EXISTE` | Despacho inexistente |
| 409 | `CUPO_INSUFICIENTE` | Sin cupo atómico |
| 409 | `ESTADO_INVALIDO` | Confirmación en estado incorrecto |
| 422 | `ZONA_RIESGOSA` | Score de riesgo > 80 (cupo liberado) |

---

## Esquema de base de datos

Script: [`deployment/01-schema.sql`](deployment/01-schema.sql).

| Tabla | Rol |
|-------|-----|
| `vehiculo` | Flota; `cupo_kg` disponible y `reservado_kg` en asignaciones |
| `despacho` | Solicitud; estados del flujo; `idem_key` idempotencia; `expira_en` para job |
| `paquete` | Paquetes ligados a despacho y vehículo |

### Modificaciones al esquema (respecto al borrador inicial)

| Cambio | Justificación |
|--------|----------------|
| `CHECK` en `despacho.estado` | Solo permite los estados del taller; evita typos o estados inválidos que romperían confirmación, SSE (`takeUntil`) y el job de expiración. |
| `CHECK` en `score_riesgo` (0–100 o NULL) | Alineado con la regla de negocio (umbral 80); rechaza datos incoherentes antes de persistir. |
| Índice parcial `idx_despacho_asignado_expira` | El job cada 30 s busca `ASIGNADO` con `expira_en < now()`; el índice parcial acota filas indexadas y reduce I/O frente al índice compuesto genérico. |
| Comentarios en índices existentes | Documentan qué consulta reactiva usa cada índice (job, GET despacho, saga). |

**Nota:** Si ya tenías el volumen Docker creado, aplica el esquema nuevo con `docker compose down -v` y `up -d`, o ejecuta manualmente los `ALTER` equivalentes en tu instancia.

---

## Pruebas

```powershell
.\gradlew.bat test
```

El test de integración de cupo se omite si Postgres no está escuchando en `localhost:5432`.

| Prueba | Qué cubre |
|--------|-----------|
| `AsignacionSagaTest` | `StepVerifier`: reserva y compensación en orden inverso |
| `DespachoServiceTest` | Score > 80 libera cupo; score 80 asigna; idempotencia; `trazaId` desde el Context |
| `TransportistaClientTest` | `Mono.zip`, retry, cache, timeout + fallback |
| `ReporteServiceTest` | `TestPublisher`: agregación y `limitRate` |
| `DespachoEventBusTest` | Un `Sinks.multicast` alimenta tablero y stream del despacho |
| `ApiWebTest` | `WebTestClient`: `@Valid` 400, 422, 409, SSE, NDJSON in/out |
| `CupoAsignacionIntegrationTest` | Postgres real: cupo queda igual tras compensar |

---

## Elementos reactivos → archivo

| Elemento | Ubicación |
|----------|-----------|
| `flatMap` / `concatMap` (saga ordenada) | `AsignacionSaga.java` |
| `Mono.zip` (externos en paralelo) | `TransportistaClient.java` |
| `retryWhen(Retry.backoff(3, 200ms))` + fallback de catálogo | `TransportistaClient.java` |
| `cache(Duration)` clima 10 min | `TransportistaClient.java` |
| `timeout(800ms)` riesgo + score 50 | `TransportistaClient.java` |
| `TransactionalOperator` | `DBConfiguration.java`, `DespachoService.java`, `ExpiracionService.java` |
| UPDATE atómico `RETURNING` | `CupoService.java` |
| Reactor Context (`trazaId`, sin parámetro) | `TrazaWebFilter.java`, `TrazaContext.java` |
| Job `Flux.interval(30s)` + `onBackpressureDrop` | `ExpiracionJob.java` |
| Errores reactivos | `GlobalErrorHandler.java` |
| `Sinks.many().multicast()` | `DespachoEventBus.java` |
| SSE por despacho (`takeUntil` terminal) | `ShippingController.java` |
| Tablero hot | `OpsController.java` |
| NDJSON in, lotes de 500, `ON CONFLICT DO UPDATE` | `VehiculoService.java`, `VehicleController.java` |
| Reporte + `limitRate` + NDJSON out | `ReporteService.java`, `ReportController.java` |

### Correcciones aplicadas

| Punto | Qué estaba mal | Qué hace ahora |
|-------|----------------|----------------|
| Alta de vehículo | El id lo manda el cliente. Spring Data lo tomaba como update y no insertaba | `VehiculoEntity` implementa `Persistable` |
| Saga | La lista de reservas vivía fuera del `Mono` y la compensación iba hacia adelante | `Mono.defer` + liberación en orden inverso |
| Idempotencia | Un `RECIBIDO` a medias (falló el cupo) se devolvía como si el despacho hubiera quedado listo | Solo se reutiliza la clave si el estado ya no es `RECIBIDO` |
| Transacción de paquetes | `flatMap` pedía varias escrituras a la vez sobre la conexión de la transacción | `concatMap` dentro del `TransactionalOperator` |
| Confirmación y expiración | Cupo y estado podían quedar a medias, y no había evento | Misma transacción y publicación al bus |
| F5 | SSE, tablero, NDJSON y reporte devolvían vacío o `null` | Bus multicast, streams y agregación con backpressure |
| Traza | El id se copiaba como argumento de método | Se lee del Reactor Context en el log y al persistir |

---
