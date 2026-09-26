# Analisis financiero

Que puede decir LUMA sobre el dinero de una persona sin apoyarse en una IA:
por que no alcanzo un ciclo, a donde podria ir lo que sobra, y que categorias
llevan una racha de gasto al alza.

**Estado:** API y tarjeta del resumen en la Fase 13 -solo la parte
determinista-. Todo automatizable hoy.

La capa de redaccion y priorizacion con LLM que describe
[`00-arquitectura-fase-0.md`](../00-arquitectura-fase-0.md) (S8.4) queda
**pendiente**, por decision explicita: por ahora LUMA se sostiene con reglas
fijas y verificables, no con un modelo de lenguaje. Cuando esa capa exista, su
unico trabajo sera redactar y priorizar estas mismas cifras -nunca calcularlas-,
y este documento se actualizara para cubrirla.

---

## Las tres senales, y por que ninguna se inventa

| Senal | Aparece cuando | No aparece cuando |
|---|---|---|
| Causa del deficit | El ciclo activo esta en deficit y hay un ciclo anterior con que comparar | No hay ciclo abierto, el ciclo no esta en deficit, o no hay ciclo anterior |
| Reparto del remanente | El ciclo activo tiene remanente | No hay ciclo abierto, o el ciclo no tiene remanente |
| Crecimiento sostenido | Una categoria subio en cada uno de los ultimos 3 ciclos, y el ultimo monto es positivo | Hay menos de 3 ciclos de historia, o ninguna categoria subio en los tres |

Las tres son independientes entre si y todas pueden faltar a la vez. Un ciclo
en equilibrio, por ejemplo, no tiene ni causa de deficit ni reparto de
remanente. No es un error ni una lista vacia por accidente: es exactamente lo
que hay que mostrar cuando no hay nada honesto que senalar.

La causa del deficit compara categorias, no ingresos contra gastos: es una
afirmacion mas angosta ("esta categoria subio, y por esto") pero es la unica
que las cifras respaldan sin adivinar. El deficit puede venir de varias cosas a
la vez, o de un ingreso que bajo, y el analisis no pretende explicar todo eso.

El reparto del remanente sigue la misma logica de prioridad que ya usa el
consejo de deficit ([`12-resumen-financiero.md`](12-resumen-financiero.md)),
en la direccion contraria: en vez de recortar de lo que menos duele a lo que
mas, reparte de la meta mas prioritaria a la menos, llenando cada una antes de
seguir con la siguiente. No mueve nada: es una sugerencia que la persona sigue
o no desde Ahorros.

## Nunca se sugiere retrasar un pago critico

Esta propiedad ya la garantiza `DeficitAdvisor` desde la Fase 10 -ver
[`12-resumen-financiero.md`](12-resumen-financiero.md)-, y el analisis
financiero no la duplica: el modulo de deficit nunca ofrece un gasto critico
como candidato de recorte, y este documento no repite ese escenario.

## Cada recomendacion muestra las cifras que la respaldan

No es un escenario aparte: es una propiedad de todos los de abajo. La causa
del deficit siempre trae el nombre de la categoria y su monto anterior y
actual; el reparto siempre trae el monto exacto por meta; el crecimiento
siempre trae el primer y el ultimo monto de la racha. Ninguna oracion en la
interfaz lleva un numero que no venga del servidor.

---

## La API

```gherkin
# language: es

@api @analisis
Caracteristica: Consultar el analisis financiero
  Como persona con una cuenta en LUMA
  Quiero entender por que me fue como me fue este ciclo
  Para decidir que hacer con lo que sobra o con lo que falta

  @listo @smoke
  Escenario: Sin ciclo abierto, ninguna senal aparece
    Dado una cuenta sin ningun ciclo abierto
    Cuando consulto mi analisis financiero
    Entonces no hay causa de deficit
    Y no hay reparto de remanente
    Y no hay ninguna categoria con crecimiento sostenido

  @listo @critico
  Escenario: El deficit se explica con la categoria que mas subio
    Dado un ciclo activo en deficit
    Y un ciclo anterior donde una categoria subio mas que las demas
    Cuando consulto mi analisis financiero
    Entonces la causa del deficit senala esa categoria
    Y trae su monto en el ciclo anterior y en el actual

  @listo
  Escenario: Sin ciclo anterior con que comparar, no se afirma ninguna causa
    Dado un ciclo activo en deficit, el primero de la cuenta
    Cuando consulto mi analisis financiero
    Entonces no hay causa de deficit

  @listo @critico
  Escenario: El remanente se reparte en cascada por prioridad
    Dado un ciclo activo con remanente
    Y dos metas activas, una mas prioritaria que la otra
    Cuando consulto mi analisis financiero
    Entonces el reparto llena primero la meta mas prioritaria
    Y manda el resto a la siguiente

  @listo
  Escenario: El remanente aparece aunque no haya ninguna meta activa
    Dado un ciclo activo con remanente
    Y ninguna meta activa
    Cuando consulto mi analisis financiero
    Entonces el remanente aparece con su monto
    Y el reparto viene vacio

  @listo @critico
  Escenario: Una categoria con 3 ciclos seguidos al alza aparece en el analisis
    Dado tres ciclos consecutivos donde una categoria subio en cada uno
    Cuando consulto mi analisis financiero
    Entonces esa categoria aparece con crecimiento sostenido
    Y trae el monto del primero y del ultimo ciclo

  @listo
  Escenario: Con menos de 3 ciclos de historia, no se afirma ningun crecimiento
    Dado solo dos ciclos de historia para una categoria
    Cuando consulto mi analisis financiero
    Entonces no hay ninguna categoria con crecimiento sostenido

  @listo
  Escenario: Una caida entre dos ciclos rompe la racha
    Dado tres ciclos donde una categoria subio, bajo y volvio a subir
    Cuando consulto mi analisis financiero
    Entonces esa categoria no aparece con crecimiento sostenido
```

## La interfaz

```gherkin
# language: es

@ui @analisis
Caracteristica: La tarjeta de analisis financiero
  Como persona usando LUMA
  Quiero ver estas senales sin salir del resumen financiero
  Para no tener que interpretarlas yo misma

  @listo
  Escenario: Sin ninguna senal, la tarjeta no aparece
    Dado un ciclo activo balanceado y sin historial suficiente
    Cuando visito el resumen financiero
    Entonces no hay ninguna tarjeta de analisis financiero

  @listo @critico
  Escenario: Con una causa de deficit, se lee la categoria y sus montos
    Dado un analisis financiero con causa de deficit
    Cuando visito el resumen financiero
    Entonces la tarjeta muestra la categoria y sus dos montos

  @listo @critico
  Escenario: Con un reparto de remanente, se listan las metas en orden
    Dado un analisis financiero con reparto entre dos metas
    Cuando visito el resumen financiero
    Entonces la tarjeta lista ambas metas en el orden en que se llenaron

  @listo
  Escenario: Con crecimiento sostenido, se listan las categorias
    Dado un analisis financiero con una categoria en racha
    Cuando visito el resumen financiero
    Entonces la tarjeta la muestra con su primer y su ultimo monto
```

---

## Cobertura automatizada hoy

| Prueba | Que cubre |
|---|---|
| `CategoryGrowthDetectorTest` | La definicion exacta de "sostenido": cada ciclo mas caro que el anterior, sin excepcion, y el ultimo positivo |
| `DeficitCauseFinderTest` | Que se elija la categoria con el mayor incremento, con desempate estable, y que sin incremento en ninguna categoria el resultado sea vacio |
| `SurplusAllocatorTest` | El reparto en cascada por prioridad, incluida la meta que no alcanza a llenarse y el caso sin remanente |
| `InsightsIntegrationTest` | Las tres senales contra MySQL real, incluidos los tres casos de honestidad: sin ciclo anterior, sin metas activas, y con menos de 3 ciclos de historia |
| `FinancialInsightsCard.test.tsx` | Que la tarjeta no aparezca sin ninguna senal, y que cada senal presente muestre sus propias cifras |

Los escenarios `@ui` de arriba describen el comportamiento visible; la prueba
de componente cubre lo mismo sin necesitar un navegador.
