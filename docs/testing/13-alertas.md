# Alertas

Lo que LUMA vigila por su cuenta, sin que la persona tenga que ir a buscarlo:
un pago que se acerca, uno que ya se paso, o un ciclo que no va a alcanzar.

**Estado:** API y campana en la Fase 11. Todo automatizable hoy.

---

## Las tres condiciones y cuando avisan

| Tipo | Se dispara cuando | Lo detecta |
|---|---|---|
| `PAYMENT_DUE_SOON` | Un renglon pendiente vence en exactamente 3 dias | `PaymentAlertsJob`, una vez al dia |
| `PAYMENT_OVERDUE` | Un renglon acaba de marcarse vencido | El mismo instante en que `OverdueItemsJob` lo marca |
| `CYCLE_DEFICIT` | El balance PRESUPUESTADO del ciclo activo es negativo | `PaymentAlertsJob`, una vez al dia |

Tres dias es una constante, no una preferencia configurable: es la decision mas
simple que cumple el escenario, y no hay todavia ninguna razon de producto para
ofrecer otra cosa.

## Una alerta por condicion, no una por dia

Mientras un pago siga vencido o un ciclo siga en deficit, el job programado lo
vuelve a ver todos los dias, y aun asi solo existe UNA fila. Lo garantiza una
llave unica (usuario, tipo, referencia): la primera vez que se detecta se crea
la alerta, las siguientes veces no pasa nada. Se resuelve sola cuando la
referencia cambia: un ciclo se cierra y da paso a otro con otro identificador,
y ahi si puede volver a dispararse.

Esto es intencional y tiene una consecuencia visible: marcar una alerta como
leida NO la hace reaparecer al dia siguiente. Si la condicion sigue activa, la
persona ya la vio; repetirsela seria ruido, no ayuda.

## Ninguna cifra se calcula en el cliente

El servidor manda el monto (lo planeado del renglon, o lo que falta en el
ciclo) y la fecha, ya calculados. La oracion que se lee, por ejemplo "Vence
pronto: Renta" o "Tu ciclo no alcanza", la arma el cliente segun el tipo, con un
mapa fijo, igual que STATE_HEADLINE en el resumen financiero. Ninguna resta,
ningun conteo de dias: lo unico que el cliente hace con una fecha es darle
formato.

---

## La API

```gherkin
# language: es

@api @alertas
Caracteristica: Consultar y atender las alertas
  Como persona con una cuenta en LUMA
  Quiero ver lo que el sistema detecto por mi
  Para no tener que revisar cada pago a mano

  @listo @smoke @critico
  Escenario: Sin alertas, la lista viene vacia y el contador en cero
    Dado una cuenta sin ninguna condicion detectada
    Cuando consulto mis alertas
    Entonces la lista viene vacia
    Y el contador de no leidas es 0

  @listo @critico
  Escenario: Un renglon que vence en 3 dias genera una alerta
    Dado un renglon pendiente que vence en exactamente 3 dias
    Cuando corre el trabajo programado
    Entonces aparece una alerta de tipo pago proximo
    Y trae el nombre, el monto y la fecha del renglon

  @listo
  Escenario: Un renglon que vence en 2 o en 4 dias no genera nada
    Dado un renglon pendiente que vence en 2 dias y otro en 4
    Cuando corre el trabajo programado
    Entonces no se genera ninguna alerta

  @listo @critico
  Escenario: Un renglon que se marca vencido genera una alerta al instante
    Dado un renglon pendiente cuya fecha ya paso
    Cuando corre el trabajo que marca vencidos
    Entonces el renglon queda vencido
    Y aparece una alerta de tipo pago vencido

  @listo @critico
  Escenario: Un ciclo que no alcanza genera una alerta con el faltante exacto
    Dado un ciclo activo cuyo balance presupuestado es negativo
    Cuando corre el trabajo programado
    Entonces aparece una alerta de tipo deficit
    Y el monto es justo lo que falta, en positivo

  @listo
  Escenario: Un ciclo que si alcanza no genera nada
    Dado un ciclo activo cuyo balance presupuestado es positivo o cero
    Cuando corre el trabajo programado
    Entonces no se genera ninguna alerta de deficit

  @listo @critico
  Escenario: La misma condicion no se repite al otro dia
    Dado un renglon ya marcado como vencido, con su alerta generada
    Cuando el trabajo que marca vencidos vuelve a correr
    Entonces sigue existiendo una sola alerta para ese renglon

  @listo
  Escenario: Marcar una alerta como leida reduce el contador
    Dado una alerta sin leer
    Cuando la marco como leida
    Entonces el contador de no leidas baja en uno

  @listo
  Escenario: Marcar todas como leidas deja el contador en cero
    Dado dos alertas sin leer
    Cuando marco todas como leidas
    Entonces el contador de no leidas es 0
    Y ambas aparecen como leidas
```

## La interfaz

```gherkin
# language: es

@ui @alertas
Caracteristica: La campana del encabezado
  Como persona usando LUMA
  Quiero ver mis alertas sin salir de donde estoy
  Para no tener que revisar cada seccion por separado

  @listo @smoke @critico
  Escenario: La insignia solo aparece con alertas sin leer
    Dado una cuenta sin alertas sin leer
    Cuando miro la campana
    Entonces no hay insignia

  @listo @critico
  Escenario: La insignia muestra cuantas hay sin leer
    Dado tres alertas sin leer
    Cuando miro la campana
    Entonces la insignia dice 3

  @listo
  Escenario: Abrir la campana sin alertas muestra un estado vacio
    Dado una cuenta sin ninguna alerta
    Cuando abro la campana
    Entonces se dice que no hay alertas por ahora

  @listo @critico
  Escenario: Cada alerta se lee segun su tipo, sin cifras fabricadas
    Dado una alerta de pago proximo, una de vencido y una de deficit
    Cuando abro la campana
    Entonces cada una muestra su propio texto y su propio monto

  @listo @critico
  Escenario: Abrir una alerta la marca como leida y manda a Este ciclo
    Dado una alerta sin leer
    Cuando la selecciono desde el menu
    Entonces queda marcada como leida
    Y la pantalla cambia a "Este ciclo"

  @listo
  Escenario: El boton de marcar todas solo aparece si hay algo sin leer
    Dado que ya no queda ninguna alerta sin leer
    Cuando abro la campana
    Entonces no aparece el boton de marcar todas
```

---

## Cobertura automatizada hoy

| Prueba | Que cubre |
|---|---|
| `NotificationTest` | La entidad sin base de datos: que trae cada tipo, que nunca se comparte un public_id, y que marcar como leida es idempotente |
| `NotificationIntegrationTest` | Los tres disparadores contra MySQL real: el umbral exacto de 3 dias, el calculo del faltante en deficit, el evento de vencido, que ninguno se duplica al correr dos veces, y la consulta (listar, contar, marcar una o todas) |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP y la campana,
que las pruebas de Java no ejercitan.
