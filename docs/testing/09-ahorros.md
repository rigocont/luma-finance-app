# Ahorros

Metas de ahorro: cuánto quieres juntar, cuánto llevas y cuánto se aparta en cada
ciclo para llegar.

**Estado:** API y pantalla en la Fase 8. Todo automatizable hoy.

---

## Las tres formas de ahorrar, y por qué son tres

El modo de aporte no es una preferencia estética: decide si la meta **resta del
presupuesto** y quién calcula cuánto.

| Modo | Quién decide el aporte | ¿Entra al ciclo? |
|---|---|---|
| `AUTO_BY_TARGET_DATE` | LUMA: lo que falta ÷ ciclos restantes | Sí |
| `FIXED_PER_CYCLE` | La persona, un monto fijo | Sí |
| `MANUAL` | Nadie: se aporta cuando se puede | No |

De ahí salen los campos obligatorios: sin fecha objetivo no hay entre cuántos
ciclos repartir, y sin monto no hay aporte fijo que apartar. La pantalla pide
solo el campo del modo elegido.

`plannedPerCycle` **solo está guardado** cuando la persona lo fijó. Con fecha
objetivo se recalcula en cada ciclo, porque depende de cuánto falta hoy: la
tarjeta muestra la fecha objetivo en vez de un cero que no significaría nada, y
el monto real aparece como renglón del ciclo.

---

## El doble conteo es el riesgo de este módulo

Un aporte puede entrar por dos caminos: confirmando el renglón de ahorro del
ciclo, o registrándolo a mano. Si los dos suman sobre el mismo evento, el
progreso miente y no hay forma de notarlo a simple vista.

La defensa es `savings_contributions.cycle_item_id`: el movimiento que nació de
un renglón guarda cuál fue. Confirmar dos veces el mismo renglón no vuelve a
sumar.

---

## Alta

```gherkin
# language: es

@api @ahorros
Característica: Crear una meta de ahorro
  Como persona que quiere juntar para algo
  Quiero decir cuánto y para cuándo
  Para que LUMA me diga cuánto apartar en cada ciclo

  @listo @smoke @critico
  Escenario: Alta con fecha objetivo
    Cuando creo una meta con nombre, monto objetivo y fecha
    Entonces la respuesta es 201
    Y la meta se identifica con un UUID
    Y queda en marcha
    Y el progreso es cero

  @listo @critico
  Escenario: Alta con aporte fijo por ciclo
    Cuando creo una meta indicando cuánto apartaré en cada ciclo
    Entonces la respuesta es 201
    Y el cuerpo devuelve ese aporte por ciclo

  @listo
  Escenario: Alta en modo manual
    Cuando creo una meta indicando que aportaré cuando pueda
    Entonces la respuesta es 201
    Y el cuerpo indica que NO resta del presupuesto

  @listo @critico
  Escenario: El cálculo automático exige una fecha objetivo
    Cuando creo una meta con cálculo automático y sin fecha
    Entonces la respuesta es 422
    Y el mensaje dice que hace falta una fecha objetivo

  @listo @critico
  Escenario: El aporte fijo exige un monto
    Cuando creo una meta con aporte fijo y sin monto por ciclo
    Entonces la respuesta es 422
    Y el mensaje dice que hay que indicar cuánto apartar

  @listo
  Escenario: Una meta en cero se rechaza
    Cuando creo una meta con monto objetivo 0
    Entonces la respuesta es 422

  @listo
  Escenario: Un monto con tres decimales se rechaza
    Cuando creo una meta con monto objetivo "1000.555"
    Entonces la respuesta es 400
    Y el error señala el campo del monto

  @listo @seguridad
  Escenario: Crear una meta exige sesión
    Dado que no estoy autenticado
    Cuando creo una meta
    Entonces la respuesta es 401
```

---

## Relación con el ciclo abierto

Misma regla que ingresos y gastos: **lo nuevo entra, lo editado no.**

```gherkin
# language: es

@api @ahorros @ciclos
Característica: La meta y el ciclo en curso
  Como persona que captura una meta a media quincena
  Quiero verla reflejada de inmediato
  Para que el balance de hoy signifique algo

  @listo @critico
  Escenario: Una meta nueva entra al ciclo en curso
    Dado que tengo un ciclo abierto
    Cuando creo una meta que resta del presupuesto
    Entonces el ciclo gana un renglón de ahorro

  @listo
  Escenario: El aporte se espera al cierre del ciclo
    Dado que tengo un ciclo abierto
    Cuando creo una meta que resta del presupuesto
    Entonces el renglón vence el último día del ciclo
    Y queda como flexible

  @listo @critico
  Escenario: Una meta en modo manual NO entra al ciclo
    Dado que tengo un ciclo abierto
    Cuando creo una meta en modo manual
    Entonces el ciclo no gana ningún renglón

  @listo
  Escenario: Sin ciclo abierto no pasa nada
    Dado que no tengo ningún ciclo abierto
    Cuando creo una meta
    Entonces la respuesta es 201
    Y no se crea ningún renglón

  @listo @critico
  Escenario: Editar la meta no reescribe el renglón que ya existe
    Dado una meta ya materializada en el ciclo abierto
    Cuando cambio su monto objetivo
    Entonces el renglón del ciclo conserva el aporte que tenía
```

---

## Confirmar el renglón de ahorro

Aquí es donde el ciclo y la meta se tocan. Es el camino normal de un aporte.

```gherkin
# language: es

@api @ahorros @ciclos
Característica: Confirmar el aporte de ahorro de un ciclo
  Como persona que cerró su quincena
  Quiero confirmar cuánto aparté de verdad
  Para que el progreso de la meta sea real y no un plan

  @listo @smoke @critico
  Escenario: Confirmar el renglón sube el progreso de la meta
    Dado un ciclo abierto con un renglón de ahorro de 1000
    Cuando confirmo ese renglón por 1000
    Entonces el progreso de la meta sube 1000
    Y queda un movimiento del tipo "del ciclo"

  @listo @critico
  Escenario: Confirmar por un monto distinto registra lo que de verdad aparté
    Dado un ciclo abierto con un renglón de ahorro de 1000
    Cuando confirmo ese renglón por 600
    Entonces el progreso de la meta sube 600
    Y el renglón queda como parcial

  @listo @critico
  Escenario: Con el aporte desmarcado NO se toca la meta
    Dado un ciclo abierto con un renglón de ahorro
    Cuando confirmo el renglón pidiendo no registrarlo en la meta
    Entonces el renglón queda confirmado
    Y el progreso de la meta no cambia

  @listo @critico
  Escenario: Confirmar dos veces NO suma dos veces
    Dado un renglón de ahorro ya confirmado
    Cuando lo confirmo otra vez
    Entonces el progreso de la meta sigue igual
    Y sigue habiendo un solo movimiento

  @listo
  Escenario: Si la meta se eliminó, el renglón se confirma igual
    Dado un ciclo abierto con un renglón de ahorro
    Y que eliminé la meta después de abrir el ciclo
    Cuando confirmo el renglón
    Entonces la respuesta es 200
    Y no se registra ningún movimiento
```

---

## Movimientos sueltos

```gherkin
# language: es

@api @ahorros
Característica: Aportaciones y retiros fuera del ciclo
  Como persona a la que le llegó un dinero extra
  Quiero sumarlo a una meta sin esperar al ciclo
  Para que el progreso refleje lo que tengo

  @listo @critico
  Escenario: Una aportación extra sube el progreso
    Dado una meta con 5000 juntados
    Cuando registro una aportación de 1000
    Entonces el progreso es 6000

  @listo @critico
  Escenario: Un retiro baja el progreso
    Dado una meta con 5000 juntados
    Cuando registro un retiro de 2000
    Entonces el progreso es 3000
    Y el movimiento se guarda en negativo

  @listo @critico
  Escenario: No se puede retirar más de lo que hay
    Dado una meta con 5000 juntados
    Cuando registro un retiro de 6000
    Entonces la respuesta es 422
    Y el progreso sigue en 5000

  @listo
  Escenario: El monto viaja siempre positivo
    Cuando registro un movimiento con monto negativo
    Entonces la respuesta es 400
    Y el error señala el campo del monto

  @listo
  Escenario: Sin fecha se usa la de hoy
    Cuando registro una aportación sin fecha
    Entonces el movimiento queda con la fecha de hoy

  @listo
  Escenario: El historial viene del más reciente al más antiguo
    Dado una meta con tres movimientos en días distintos
    Cuando consulto sus movimientos
    Entonces el primero es el más reciente

  @listo
  Escenario: Los movimientos que vienen del ciclo se distinguen
    Dado una meta con un aporte del ciclo y una aportación extra
    Cuando consulto sus movimientos
    Entonces uno está marcado como originado en un ciclo
```

---

## Alcanzar la meta

```gherkin
# language: es

@api @ahorros
Característica: Una meta alcanzada
  Como persona que llegó a su objetivo
  Quiero que LUMA lo note
  Para dejar de apartar dinero que ya no hace falta

  @listo @critico
  Escenario: Llegar al objetivo marca la meta como alcanzada
    Dado una meta de 10000 con 9000 juntados
    Cuando registro una aportación de 1000
    Entonces la meta queda como alcanzada

  @listo
  Escenario: Pasarse del objetivo no deja lo que falta en negativo
    Dado una meta de 10000 con 9000 juntados
    Cuando registro una aportación de 3000
    Entonces lo que falta es 0
    Y el progreso mostrado es 100%

  @listo
  Escenario: Subir el objetivo vuelve a poner en marcha una meta alcanzada
    Dado una meta ya alcanzada
    Cuando subo su monto objetivo
    Entonces la meta vuelve a estar en marcha

  @listo
  Escenario: Una meta alcanzada deja de generar renglón en el ciclo
    Dado una meta ya alcanzada
    Cuando abro el siguiente ciclo
    Entonces no aparece su renglón de ahorro
```

---

## Prioridad

```gherkin
# language: es

@api @ahorros
Característica: El orden de las metas
  Como persona con varias metas
  Quiero decir cuál va primero
  Para que un remanente se reparta como yo quiero

  @listo
  Escenario: La meta nueva queda al final
    Dado que ya tengo una meta
    Cuando creo otra
    Entonces la nueva queda después de la primera

  @listo @critico
  Escenario: Reordenar reasigna las prioridades en el orden recibido
    Dado dos metas en cierto orden
    Cuando mando el orden invertido
    Entonces la respuesta las devuelve invertidas

  @listo @critico
  Escenario: Un orden incompleto se rechaza
    Dado dos metas
    Cuando mando un orden con una sola
    Entonces la respuesta es 422

  @listo @critico
  Escenario: Un orden con una meta repetida se rechaza
    Dado dos metas
    Cuando mando un orden que repite la misma dos veces
    Entonces la respuesta es 422
```

---

## Pausar, reanudar y eliminar

```gherkin
# language: es

@api @ahorros
Característica: Ciclo de vida de una meta
  Como persona que necesita frenar un ahorro
  Quiero pausarlo sin perder lo que llevo
  Para retomarlo cuando pueda

  @listo @critico
  Escenario: Pausar deja de restar del presupuesto
    Dado una meta en marcha
    Cuando la pauso
    Entonces deja de restar del presupuesto
    Y conserva lo que llevaba juntado

  @listo
  Escenario: Una meta pausada no entra al siguiente ciclo
    Dado una meta pausada
    Cuando abro el siguiente ciclo
    Entonces no aparece su renglón de ahorro

  @listo
  Escenario: Reanudar la vuelve a poner en marcha
    Dado una meta pausada
    Cuando la reanudo
    Entonces vuelve a restar del presupuesto

  @listo @critico
  Escenario: Eliminar es borrado lógico
    Dado una meta con movimientos
    Cuando la elimino
    Entonces la respuesta es 204
    Y desaparece de mi lista
    Y los ciclos anteriores siguen mostrando su renglón

  @listo @seguridad
  Escenario: No se ven las metas de otra persona
    Dado una meta de otra cuenta
    Cuando la consulto con mi sesión
    Entonces la respuesta es 404
```

---

## Edición

```gherkin
# language: es

@api @ahorros
Característica: Editar una meta
  Como persona cuyos planes cambiaron
  Quiero ajustar el monto o la fecha
  Para que el plan siga siendo el mío

  @listo
  Escenario: Se manda solo lo que cambia
    Cuando mando solo el nombre nuevo
    Entonces el resto de la meta no cambia

  @listo @critico
  Escenario: Quitar la fecha objetivo necesita una bandera
    Dado una meta con fecha objetivo
    Cuando mando la bandera de quitar fecha
    Entonces la meta queda sin fecha

  @listo @critico
  Escenario: Cambiar de modo sin mandar el monto conserva el que ya había
    Dado una meta con aporte fijo de 1500
    Cuando cambio de modo y vuelvo al aporte fijo sin mandar monto
    Entonces el aporte sigue siendo 1500

  @listo
  Escenario: Pasar a cálculo automático sin fecha se rechaza
    Dado una meta con aporte fijo y sin fecha objetivo
    Cuando la cambio a cálculo automático
    Entonces la respuesta es 422
```

---

## La pantalla

```gherkin
# language: es

@ui @ahorros
Característica: Pantalla de ahorros
  Como persona que quiere ver cómo va lo que junto
  Quiero mis metas en una sola vista
  Para saber en qué voy sin sacar cuentas

  @listo @smoke
  Escenario: La sección se llega desde el menú
    Dado que inicié sesión
    Cuando abro "Ahorros" en el menú
    Entonces veo la pantalla de ahorros

  @listo
  Escenario: Sin metas se invita a crear la primera
    Dado que no tengo metas
    Cuando abro la pantalla de ahorros
    Entonces veo un estado vacío
    Y ofrece crear la primera

  @listo @critico
  Escenario: Cada meta muestra progreso, lo juntado y lo que falta
    Dado una meta de 30000 con 7500 juntados
    Cuando abro la pantalla de ahorros
    Entonces la tarjeta muestra 25%
    Y muestra que faltan 22,500

  @listo
  Escenario: Una meta con fecha objetivo muestra para cuándo es
    Dado una meta con cálculo automático y fecha objetivo
    Cuando abro la pantalla de ahorros
    Entonces la tarjeta muestra la fecha objetivo

  @listo
  Escenario: Una meta con aporte fijo muestra cuánto por ciclo
    Dado una meta con aporte fijo de 1000
    Cuando abro la pantalla de ahorros
    Entonces la tarjeta muestra "1,000.00 por ciclo"

  @listo
  Escenario: Una meta alcanzada se señala
    Dado una meta alcanzada
    Cuando abro la pantalla de ahorros
    Entonces la tarjeta lleva la marca de alcanzada

  @listo @critico
  Escenario: El formulario solo pide el campo del modo elegido
    Cuando elijo cálculo automático
    Entonces se pide la fecha objetivo
    Y no se pide el monto por ciclo

  @listo
  Escenario: El modo manual avisa que no resta del presupuesto
    Cuando elijo aportar cuando pueda
    Entonces aparece un aviso de que no entra al presupuesto

  @listo @critico
  Escenario: Un error de validación deja el cajón abierto
    Cuando guardo una meta sin nombre
    Entonces el cajón sigue abierto
    Y el error aparece junto al campo del nombre

  @listo @critico
  Escenario: Registrar una aportación desde el menú de la tarjeta
    Cuando abro el menú de una meta y elijo "Registrar aportación"
    Y capturo el monto y guardo
    Entonces aparece un aviso de confirmación
    Y el progreso de la tarjeta sube

  @listo @critico
  Escenario: Alcanzar la meta se dice con nombre y apellido
    Dado una meta a la que le falta justo una aportación
    Cuando registro esa aportación
    Entonces el aviso dice que alcancé la meta

  @listo
  Escenario: Ver los movimientos abre el historial
    Cuando abro el menú de una meta y elijo "Ver movimientos"
    Entonces veo sus aportaciones y retiros
    Y los que vinieron del ciclo están señalados

  @listo
  Escenario: Una meta sin movimientos muestra un estado vacío
    Dado una meta recién creada
    Cuando abro sus movimientos
    Entonces veo un estado vacío, no un error

  @listo @critico
  Escenario: Eliminar pide confirmación
    Cuando abro el menú de una meta y elijo Eliminar
    Entonces aparece un diálogo con el nombre de la meta
    Y ofrece "Pausar" como alternativa
```

---

## Reordenar: arrastrar, y también sin arrastrar

El arrastre usa la API nativa de HTML5, sin librería. **No funciona en pantallas
táctiles**, así que cada tarjeta lleva además botones de subir y bajar. No son
una alternativa de segunda: en teléfono y con teclado son la única forma.

Para la automatización eso significa dos caminos distintos que deben probarse
por separado — un `dragTo` que pasa en escritorio no dice nada de lo que ocurre
en móvil.

```gherkin
# language: es

@ui @ahorros
Característica: Reordenar las metas por prioridad
  Como persona con varias metas
  Quiero poner primero la que más me urge
  Para que un remanente se reparta como yo quiero

  @listo @critico
  Escenario: Arrastrar una meta cambia su prioridad
    Dado tres metas en orden
    Cuando arrastro la tercera hasta el primer lugar
    Entonces queda primera
    Y aparece un aviso de orden guardado

  @listo @critico
  Escenario: El orden se conserva al recargar
    Dado que reordené mis metas
    Cuando recargo la página
    Entonces el orden es el que dejé

  @listo @critico @responsive
  Escenario: En teléfono se reordena con los botones
    Dado un viewport de teléfono
    Y tres metas en orden
    Cuando presiono "subir" en la tercera
    Entonces queda en segundo lugar

  @listo
  Escenario: La primera no se puede subir
    Cuando miro la primera meta
    Entonces su botón de subir está deshabilitado

  @listo
  Escenario: La última no se puede bajar
    Cuando miro la última meta
    Entonces su botón de bajar está deshabilitado

  @listo
  Escenario: Con una sola meta no se ofrece reordenar
    Dado una sola meta
    Cuando abro la pantalla de ahorros
    Entonces no aparece el aviso de prioridad
```

---

## Cobertura automatizada hoy

| Prueba | Qué cubre |
|---|---|
| `SavingsGoalTest` | Validaciones por modo, progreso, retiro excesivo, alcanzar y desalcanzar la meta, pausa, edición parcial |
| `SavingsPlanCalculatorTest` | El reparto de lo que falta entre los ciclos restantes, sin centavos perdidos |
| `SavingsIntegrationTest` | Materialización en el ciclo abierto, confirmación del renglón (incluida la idempotencia), movimientos sueltos, prioridad y aislamiento entre usuarios |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP y la interfaz,
que las pruebas de Java no ejercitan.
