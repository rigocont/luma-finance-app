# Revisión del ciclo

La sección «Este ciclo»: lo que falta por definir antes de que el balance
signifique algo.

**Estado:** API y pantalla en la Fase 7. Todo automatizable hoy.

---

## Confirmar es fijar el monto, no marcar un pago

Es la distinción que gobierna toda la pantalla, y conviene tenerla clara antes
de escribir un solo escenario:

| | Confirmar el monto | Registrar el pago |
|---|---|---|
| Qué dice | «La luz este ciclo son $1,150» | «Ya la pagué» |
| Endpoint | `POST .../items/confirm-amounts` | `POST .../items/{id}/settle` |
| Estado resultante | `PENDING` | `PAID` o `PARTIAL` |
| Campo que cambia | `plannedAmount` | `actualAmount`, `settledOn` |

Juntarlos haría que el balance diera por pagado lo que nadie pagó. Un gasto
variable nace en `NEEDS_REVIEW` porque **no se sabe cuánto es**; saberlo y
haberlo pagado son dos hechos distintos, y el ciclo necesita los dos.

La confirmación masiva hace la primera columna. La segunda sigue siendo por
renglón, con su fecha.

---

## La sugerencia es el ciclo pasado, no un promedio

El campo llega con un monto ya escrito: **lo que se confirmó del mismo gasto en
el ciclo inmediatamente anterior**.

No es el promedio de los últimos seis a propósito. Un promedio esconde justo lo
que importa cuando un recibo acaba de subir — lo diluye entre cinco ciclos en
que todavía no había subido. El promedio está a un clic, en el historial, donde
se ve junto a las cifras de las que sale.

Y solo se sugiere lo **confirmado**. Un monto planeado que nadie confirmó es un
plan, no lo que costó: sugerirlo propagaría la misma estimación de ciclo en
ciclo hasta hacerla parecer un dato.

---

## Qué pide revisión

```gherkin
# language: es

@api @revision @ciclos
Característica: Los renglones que piden revisión
  Como persona que abrió un ciclo nuevo
  Quiero saber qué me falta por definir
  Para que el balance no me mienta

  @listo @smoke @critico
  Escenario: Un gasto variable nace pidiendo revisión
    Dado un gasto de monto que cambia
    Cuando abro un ciclo
    Entonces ese gasto aparece en la revisión

  @listo @critico
  Escenario: Un gasto fijo NO pide revisión
    Dado un gasto de monto estable
    Cuando abro un ciclo
    Entonces ese gasto no aparece en la revisión

  @listo
  Escenario: Un ciclo sin gastos variables no tiene nada que revisar
    Cuando consulto la revisión de un ciclo sin gastos variables
    Entonces la lista viene vacía

  @listo @critico
  Escenario: Sin ciclo abierto la respuesta es 404
    Dado que no tengo ningún ciclo abierto
    Cuando consulto la revisión
    Entonces la respuesta es 404
    Y se distingue de una lista vacía

  @listo @seguridad
  Escenario: La revisión exige sesión
    Dado que no estoy autenticado
    Cuando consulto la revisión
    Entonces la respuesta es 401
```

---

## La sugerencia de monto

```gherkin
# language: es

@api @revision
Característica: El monto que se propone
  Como persona que revisa su quincena
  Quiero que LUMA me recuerde cuánto fue la vez pasada
  Para no tener que buscar el recibo

  @listo @critico
  Escenario: Propone lo que se confirmó en el ciclo anterior
    Dado un gasto que el ciclo pasado fue de 1150
    Cuando consulto la revisión del ciclo actual
    Entonces la sugerencia es 1150
    Y viene con la fecha del ciclo de donde sale

  @listo
  Escenario: Sin ciclo anterior no hay sugerencia
    Dado que este es mi primer ciclo
    Cuando consulto la revisión
    Entonces el renglón viene sin sugerencia

  @listo @critico
  Escenario: Un monto que nadie confirmó NO se sugiere
    Dado un gasto que el ciclo pasado quedó sin confirmar
    Cuando consulto la revisión
    Entonces el renglón viene sin sugerencia

  @listo
  Escenario: Con varios ciclos atrás gana el más reciente
    Dado un gasto que fue 600 hace tres ciclos y 1150 el ciclo pasado
    Cuando consulto la revisión
    Entonces la sugerencia es 1150

  @listo
  Escenario: Un gasto que el ciclo pasado no existía no tiene sugerencia
    Dado un gasto capturado después de cerrar el ciclo anterior
    Cuando consulto la revisión
    Entonces el renglón viene sin sugerencia
```

---

## El historial de un gasto

```gherkin
# language: es

@api @revision
Característica: Lo que costó este gasto antes
  Como persona que ve un recibo más alto de lo normal
  Quiero comparar con los ciclos anteriores
  Para saber si es un salto o ya venía subiendo

  @listo @critico
  Escenario: Del más reciente al más antiguo
    Dado un gasto con tres ciclos de historia
    Cuando consulto su historial
    Entonces el primero es el ciclo más reciente

  @listo @critico
  Escenario: El ciclo en curso no entra en su propio historial
    Dado que ya registré el pago de este ciclo
    Cuando consulto el historial del renglón
    Entonces el ciclo en curso no aparece

  @listo
  Escenario: Solo aparecen los ciclos confirmados
    Dado un ciclo anterior donde el gasto quedó sin confirmar
    Cuando consulto el historial
    Entonces ese ciclo no aparece

  @listo
  Escenario: Se topa en seis ciclos
    Dado un gasto con diez ciclos de historia
    Cuando consulto su historial
    Entonces vienen seis

  @listo @critico
  Escenario: El promedio llega calculado del servidor
    Dado un gasto que costó 600 y 900
    Cuando consulto su historial
    Entonces el promedio es 750.00
    Y viene con dos decimales

  @listo
  Escenario: La primera vez, el historial viene vacío y sin promedio
    Dado un gasto que nunca se ha confirmado
    Cuando consulto su historial
    Entonces la lista viene vacía
    Y el promedio es nulo

  @listo @seguridad
  Escenario: No se ve el historial de un ciclo ajeno
    Dado el ciclo de otra cuenta
    Cuando consulto un historial con mi sesión
    Entonces la respuesta es 404
```

---

## Confirmar montos en lote

```gherkin
# language: es

@api @revision
Característica: Fijar varios montos de una vez
  Como persona que revisa la quincena entera
  Quiero confirmar todo de un golpe
  Para no guardar seis veces

  @listo @smoke @critico
  Escenario: Confirmar fija el monto y saca de revisión
    Dado un gasto en revisión
    Cuando confirmo su monto en 1150
    Entonces el renglón queda pendiente
    Y su monto planeado es 1150

  @listo @critico
  Escenario: Confirmar NO marca el renglón como pagado
    Dado un gasto en revisión
    Cuando confirmo su monto
    Entonces el renglón no tiene monto real
    Y no tiene fecha de pago

  @listo @critico
  Escenario: Si un renglón del lote falla, no se confirma ninguno
    Dado un lote con un renglón válido y uno inexistente
    Cuando lo mando
    Entonces la respuesta es 404
    Y el renglón válido conserva su monto anterior

  @listo @critico
  Escenario: Un renglón repetido en el lote se rechaza
    Dado un lote que trae el mismo renglón dos veces con montos distintos
    Cuando lo mando
    Entonces la respuesta es 422
    Y el mensaje dice cuál viene repetido

  @listo
  Escenario: Un lote vacío se rechaza
    Cuando mando un lote sin renglones
    Entonces la respuesta es 400

  @listo
  Escenario: Un monto con tres decimales se rechaza
    Cuando confirmo un monto de "100.555"
    Entonces la respuesta es 400
    Y el error señala ese renglón

  @listo @critico
  Escenario: No se puede confirmar en un ciclo cerrado
    Dado un ciclo cerrado
    Cuando confirmo un monto
    Entonces la respuesta es 422
```

---

## Registrar el aporte de ahorro

El renglón de ahorro del ciclo no pide revisión —su monto lo calculó el motor—
pero sí espera que digas si de verdad apartaste el dinero. Vive en esta pantalla
porque es lo otro que el ciclo espera de ti.

```gherkin
# language: es

@api @revision @ahorros
Característica: Confirmar el aporte de ahorro del ciclo
  Como persona que ya apartó su ahorro
  Quiero registrarlo desde el mismo lugar donde reviso el ciclo
  Para no tener que ir a buscarlo a otra sección

  @listo @critico
  Escenario: El monto propuesto es el que planeó el motor
    Dado un renglón de ahorro de 1000
    Cuando abro el diálogo de registro
    Entonces el campo viene con 1000

  @listo @critico
  Escenario: Registrar sube el progreso de la meta
    Dado un renglón de ahorro de 1000
    Cuando lo registro por 1000
    Entonces el progreso de la meta sube 1000

  @listo @critico
  Escenario: Con "No registrarlo en la meta" el progreso no se mueve
    Dado un renglón de ahorro de 1000
    Cuando lo registro marcando que no cuente en la meta
    Entonces el renglón queda confirmado
    Y el progreso de la meta no cambia

  @listo
  Escenario: Registrar menos de lo planeado deja el renglón parcial
    Dado un renglón de ahorro de 1000
    Cuando lo registro por 600
    Entonces el renglón queda parcial
```

---

## La pantalla

```gherkin
# language: es

@ui @revision
Característica: La sección "Este ciclo"
  Como persona que abre LUMA a media quincena
  Quiero ver de un vistazo qué me falta por decidir
  Para que el resto de los números tengan sentido

  @listo @smoke
  Escenario: La sección se llega desde el menú
    Dado que inicié sesión
    Cuando abro "Este ciclo" en el menú
    Entonces veo la pantalla de revisión

  @listo @critico
  Escenario: El menú muestra cuántos pendientes hay
    Dado tres gastos por revisar
    Cuando miro el menú
    Entonces "Este ciclo" lleva una insignia con 3

  @listo
  Escenario: Sin pendientes no hay insignia
    Dado que no falta nada por revisar
    Cuando miro el menú
    Entonces "Este ciclo" no lleva insignia

  @listo @critico
  Escenario: Sin ciclo abierto se explica qué es un ciclo
    Dado una cuenta sin ciclos
    Cuando abro la sección
    Entonces veo un estado vacío que explica qué es un ciclo
    Y no veo un error

  @listo
  Escenario: Con todo revisado se dice que no falta nada
    Dado un ciclo sin gastos variables
    Cuando abro la sección
    Entonces veo un estado vacío
    Y sigue mostrándose el rango del ciclo

  @listo @critico
  Escenario: Cada renglón llega con el monto propuesto escrito
    Dado un gasto que el ciclo pasado fue de 1150
    Cuando abro la sección
    Entonces su campo viene con 1150
    Y dice de qué ciclo sale esa cifra

  @listo
  Escenario: Sin sugerencia el campo propone la estimación
    Dado un gasto sin historia, estimado en 800
    Cuando abro la sección
    Entonces su campo viene con 800

  @listo
  Escenario: "Usar ese monto" rellena el campo con la sugerencia
    Dado que cambié el monto de un renglón
    Cuando presiono "Usar ese monto"
    Entonces el campo vuelve a la sugerencia
    Y el botón desaparece

  @listo @critico
  Escenario: Confirmar todo guarda los montos capturados
    Dado tres gastos por revisar
    Cuando capturo los tres montos y presiono "Confirmar todo"
    Entonces aparece un aviso de confirmación
    Y la lista queda vacía

  @listo @critico
  Escenario: Un monto mal escrito no manda la petición
    Cuando escribo "abc" en un renglón y presiono "Confirmar todo"
    Entonces el error aparece junto a ese campo
    Y los demás renglones conservan lo capturado

  @listo
  Escenario: Corregir el monto limpia su error
    Dado un renglón con error de formato
    Cuando corrijo el monto
    Entonces el error desaparece

  @listo @critico
  Escenario: El historial se abre desde el renglón
    Cuando presiono el icono de historial de un gasto
    Entonces veo lo que costó en ciclos anteriores
    Y veo el promedio

  @listo
  Escenario: Un gasto sin historia lo dice
    Cuando abro el historial de un gasto nuevo
    Entonces veo que es la primera vez, no un error

  @listo @critico
  Escenario: Registrar el aporte de ahorro desde esta pantalla
    Dado un renglón de ahorro pendiente
    Cuando presiono "Registrar aporte" y guardo
    Entonces aparece un aviso de que se sumó a la meta

  @listo @critico
  Escenario: La casilla de la meta viene desmarcada
    Cuando abro el diálogo de registro de ahorro
    Entonces "No registrarlo en la meta" está desmarcado

  @listo
  Escenario: Registrar sin contarlo en la meta lo dice en el aviso
    Cuando registro marcando "No registrarlo en la meta"
    Entonces el aviso dice que no se sumó a la meta

  @listo @responsive
  Escenario: En teléfono el campo de monto ocupa el ancho completo
    Dado un viewport de teléfono
    Cuando abro la sección
    Entonces el campo de monto ocupa todo el ancho de la tarjeta
```

---

## Cobertura automatizada hoy

| Prueba | Qué cubre |
|---|---|
| `ItemHistoryEntryTest` | El promedio: dos decimales, redondeo al medio exacto, historial vacío |
| `CycleReviewIntegrationTest` | Qué pide revisión, la sugerencia entre ciclos, el historial y el lote todo-o-nada, contra MySQL real |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP y la interfaz,
que las pruebas de Java no ejercitan.
