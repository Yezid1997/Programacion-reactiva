# Decisiones

Trade-offs del taller. Cada uno está en el código, no solo aquí.

## flatMap, concatMap y Mono.defer

`flatMap` solapa el trabajo. Sirve cuando las ramas no se pisan: las tres llamadas externas van con `Mono.zip`, no con `flatMap` en serie.

`concatMap` espera a que termine la anterior. La saga reserva paquete a paquete porque la compensación recorre esa misma secuencia al revés. Dos `UPDATE` concurrentes del mismo vehículo no se pueden deshacer en orden. Dentro de `TransactionalOperator` pasa lo mismo: R2DBC presta una sola conexión, y un `flatMap` de inserts la satura.

La lista de reservas vive dentro de `Mono.defer`. Si estuviera fuera, una re-suscripción (retry, segundo subscribe del test) acumularía cupos de un intento anterior y la compensación devolvería de más.

`flatMapIterable` abre la lista de paquetes como flujo. A partir de ahí el operador que importa es `concatMap`.

## Paralelismo

`Mono.zip` se suscribe a tarifa, clima y riesgo a la vez. En el log salen los tres `inicio` antes de que vuelva cualquiera. El test `zipTardaLoDeLaLlamadaMasLenta` exige que 350 ms × 3 no se sumen.

El tablero fusiona dos fuentes con `Flux.merge`: los cambios de despacho (sink caliente) y un pulso compartido (`publish().refCount()`). El pulso es frío hasta que entra el primer monitor; cuando el último se va, el intervalo se apaga. Un `refCount` sobre los despachos no aporta: el sink multicast ya es caliente y no debe morir con el último SSE.

La suma del reporte es CPU. `publishOn(Schedulers.parallel())` la saca del hilo que lee de Postgres. `subscribeOn` movería también la suscripción a la base, y eso no queremos.

## Resiliencia

`Retry.backoff(3, 200 ms)` lleva `jitter(0.2)` para que varios clientes no reintenten en el mismo instante, y `filter` para que un 4xx no se reintente: es un error del contrato, no un fallo transitorio. El test `error4xxNoReintentaYCaeAlCatalogo` cuenta una sola petición HTTP.

Lo que queda después del retry pasa por `onErrorMap` a `TarifaNoDisponibleException` o `RiesgoNoDisponibleException`. El `onErrorResume` solo reconoce esas excepciones de dominio. Un 5xx agotado cae al catálogo; un riesgo que pasa de 800 ms cae al score 50. Un score de negocio > 80 no se disfraza de fallo técnico: es `ZonaRiesgosaException` y responde 422.

## Backpressure, una estrategia por stream

| Stream | Estrategia | Por qué |
|--------|------------|---------|
| Reporte | `limitRate(256)` | Hay que recorrer miles de filas. Pedirlas todas llena la memoria; el lote de 256 deja que Postgres entregue al ritmo del que suma. |
| Tablero | `onBackpressureLatest` | A un monitor lento le importa el último estado, no la cola de los anteriores. El test del consumidor lento recibe el primero y, al volver a pedir, el tercero: el segundo se descartó. |
| Job | `onBackpressureDrop` | Si la expiración anterior no terminó, el tick nuevo se tira. Encolar ticks atrasados solo repite el mismo barrido. |
| SSE de un despacho | buffer del sink, sin latest | Ahí sí importa no perder `ASIGNADO` antes de `EN_RUTA`. |

## Hot, heartbeat y cierre

El bus es `Sinks.many().multicast()` con `autoCancel = false`. Si el sink se cancelara al irse el último SSE, el siguiente despacho no tendría a quién hablarle y el bus quedaría muerto.

El heartbeat va cada 15 s (`Flux.merge` con los datos) para que un proxy no corte el stream en silencio. En el despacho, `takeUntil` del estado terminal completa los datos y `takeUntilOther` apaga el intervalo: si no, el SSE no cerraría nunca. `doOnCancel` / `doFinally` corren al desconectar el cliente y sueltan esa suscripción. `distinctUntilChanged` evita repetir el mismo estado cuando el `startWith` de la base y el bus entregan el mismo evento.

## Cupo y transacción

El cupo no se lee para luego escribirse. El `UPDATE ... WHERE cupo_kg >= :peso RETURNING` gana o no cambia la fila. La prueba de concurrencia dispara 20 reservas solapadas contra 500 kg: el cupo termina ≥ 0 y el reservado cuadra con las que sí entraron.

La saga devuelve lo ya tomado si el paquete de en medio falla. El guardado del despacho y sus paquetes va en `TransactionalOperator`. Si el segundo insert revienta, el test de rollback comprueba que ni el despacho ni el paquete quedan visibles.

## Traza

`TrazaWebFilter` hace `contextWrite`. Si no viene `X-Traza-Id`, genera uno y lo devuelve en la respuesta. Los logs lo leen con `deferContextual` (`TrazaContext`), sin parámetro de método. El mismo id va en el cuerpo de error y en el comentario SSE `trazaId=...` del evento.

## Demo en vivo

Romper y ver la recuperación, en este orden:

1. `PUT /external/simulator` con `{"fallasTarifa":2,"latenciaRiesgoMs":3000,"scoreRiesgo":20}`.
   La tarifa responde 503 dos veces, el retry la alcanza o cae al catálogo (log `usando catálogo`). El riesgo tarda 3 s, el timeout de 800 ms deja score 50 y el despacho sigue.
2. `PUT` con `{"fallasTarifa":0,"latenciaRiesgoMs":0,"scoreRiesgo":95}`.
   El externo responde, el score pasa de 80, la API responde 422 `ZONA_RIESGOSA` y el cupo reservado vuelve al vehículo.
3. `DELETE /external/simulator` y repetir el `POST /api/despachos`: queda `ASIGNADO`.
4. Abrir `GET /api/ops/tablero` y `GET /api/despachos/{id}/events` antes del paso 3. El tablero es el mismo bus; el del despacho se cierra en `EN_RUTA` o `RECHAZADO`. Un comentario `ping` cada 15 s es el heartbeat.
