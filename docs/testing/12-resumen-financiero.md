# Resumen financiero

La pantalla donde todo lo anterior se convierte en una respuesta: cuánto te
queda, en qué se reparte, qué sigue y — si hace falta — de dónde podría salir lo
que falta.

**Estado:** API y pantalla en la Fase 10. Todo automatizable hoy.

---

## El orden de la pantalla es el orden de las preguntas

1. **La cifra**, grande, con la frase que la interpreta debajo.
2. **De dónde podría salir** — solo si el ciclo va en déficit.
3. **En qué se reparte** lo que entra.
4. **Lo que sigue** por pagar.
5. **Comparado con el ciclo pasado.**

El déficit sube al segundo lugar a propósito: cuando hay un problema, lo que la
persona necesita no es el desglose sino qué hacer.

---

## Ninguna cifra se calcula en la interfaz

Es la regla que gobierna toda la fase, y tiene consecuencias visibles en el
contrato:

- El balance, los totales y las proporciones llegan del motor presupuestal.
- La **diferencia contra el ciclo pasado** llega como `outflowChange`, con la
  dirección (`UP` / `DOWN` / `SAME`) separada de la magnitud, que viaja siempre
  en positivo. Así la interfaz elige la frase sin restar ni sacar valores
  absolutos.
- Con déficit, el balance llega **negativo y se muestra con su signo**. No se le
  quita el menos para escribir «te falta $1,200»: eso sería fabricar una cifra a
  partir de otra.

---

## El consejo de déficit

```gherkin
# language: es

@api @resumen
Característica: De dónde podría salir lo que falta
  Como persona cuyo ciclo no cierra
  Quiero saber qué puedo mover
  Para decidir yo, con la información a la vista

  @listo @smoke @critico
  Escenario: Con remanente no propone nada
    Dado un ciclo con balance positivo
    Cuando consulto el consejo
    Entonces lo que falta es 0
    Y la lista viene vacía

  @listo @critico
  Escenario: Un gasto crítico NUNCA se propone
    Dado un déficit y un gasto crítico que alcanzaría a cubrirlo
    Cuando consulto el consejo
    Entonces ese gasto no aparece
    Y se indica que los recortes propuestos no alcanzan

  @listo @critico
  Escenario: El orden va de lo que duele menos a lo que duele más
    Dado un déficit con un gasto flexible, una meta y un gasto importante
    Cuando consulto el consejo
    Entonces primero aparece el flexible
    Y luego la meta
    Y al final el importante

  @listo @critico
  Escenario: Los ahorros van del menos prioritario al más
    Dado un déficit con tres metas de distinta prioridad
    Cuando consulto el consejo
    Entonces la primera es la que puse más abajo

  @listo
  Escenario: Dentro de un grupo, primero el monto más grande
    Dado un déficit con tres gastos flexibles de distinto monto
    Cuando consulto el consejo
    Entonces el primero es el más grande

  @listo @critico
  Escenario: Un gasto ya pagado no se propone
    Dado un déficit y un gasto flexible ya confirmado como pagado
    Cuando consulto el consejo
    Entonces ese gasto no aparece

  @listo
  Escenario: Se detiene en cuanto alcanza
    Dado un déficit de 1000 y un gasto flexible de 1200
    Cuando consulto el consejo
    Entonces se propone solo ese gasto

  @listo @critico
  Escenario: Si no alcanza, lo dice
    Dado un déficit mayor que todo lo que se puede mover
    Cuando consulto el consejo
    Entonces se indica que los recortes no cubren el hueco

  @listo
  Escenario: Un gasto variable sin confirmar se propone y se marca como estimado
    Dado un déficit y un gasto variable sin confirmar
    Cuando consulto el consejo
    Entonces aparece marcado como estimado

  @listo
  Escenario: Los ingresos nunca son candidatos
    Dado un déficit
    Cuando consulto el consejo
    Entonces ningún ingreso aparece en la lista
```

---

## La comparación entre ciclos

```gherkin
# language: es

@api @resumen
Característica: Comparar los últimos ciclos
  Como persona que quiere saber si va mejor
  Quiero ver cómo se movió mi gasto
  Para notar un cambio antes de que se vuelva costumbre

  @listo @critico
  Escenario: Del más antiguo al más reciente
    Dado tres ciclos
    Cuando consulto la comparación
    Entonces vienen ordenados del más antiguo al más reciente

  @listo
  Escenario: Sin ciclos viene vacía
    Dado una cuenta sin ciclos
    Cuando consulto la comparación
    Entonces la lista viene vacía

  @listo
  Escenario: Respeta cuántos se piden
    Dado cuatro ciclos
    Cuando pido dos
    Entonces vienen dos

  @listo
  Escenario: Se topa en doce
    Cuando pido cien ciclos
    Entonces vienen a lo más doce

  @listo @critico
  Escenario: Cada ciclo trae su balance ya calculado
    Cuando consulto la comparación
    Entonces cada ciclo trae sus totales presupuestados y reales

  @listo @critico
  Escenario: La diferencia llega con dirección y magnitud
    Dado dos ciclos donde el segundo gastó más
    Cuando consulto la comparación
    Entonces el segundo trae dirección "UP"
    Y la magnitud viene en positivo

  @listo
  Escenario: El ciclo más antiguo no trae diferencia
    Cuando consulto la comparación
    Entonces el primero de la lista no tiene con qué compararse
```

---

## La pantalla

```gherkin
# language: es

@ui @resumen
Característica: El resumen financiero
  Como persona que abre LUMA
  Quiero saber cuánto me queda
  Para no tener que sacar cuentas

  @listo @smoke @critico
  Escenario: La cifra es lo primero que se lee
    Dado un ciclo con remanente
    Cuando abro el resumen
    Entonces veo el monto disponible en grande
    Y debajo la frase que lo explica

  @listo @critico
  Escenario: Sin ciclo abierto se ofrece abrir el primero
    Dado una cuenta sin ciclos
    Cuando abro el resumen
    Entonces veo un estado vacío que explica qué es un ciclo
    Y un botón para abrirlo

  @listo @critico
  Escenario: Abrir el ciclo materializa los renglones
    Dado ingresos y gastos capturados y ningún ciclo abierto
    Cuando abro el primer ciclo
    Entonces aparece el balance
    Y la sección "Lo que sigue" trae los pagos

  @listo @critico
  Escenario: Con gastos por revisar, la cifra se declara estimación
    Dado un ciclo con dos gastos sin confirmar
    Cuando abro el resumen
    Entonces se avisa que la cifra es una estimación
    Y hay un enlace a la revisión

  @listo @critico
  Escenario: Con déficit el consejo va antes que el desglose
    Dado un ciclo en déficit
    Cuando abro el resumen
    Entonces "De dónde podría salir" aparece antes de "En qué se reparte"

  @listo @critico
  Escenario: El balance en déficit se muestra en negativo
    Dado un ciclo en déficit
    Cuando abro el resumen
    Entonces la cifra lleva su signo menos
    Y el titular dice que falta para cerrar

  @listo @critico
  Escenario: Quitar un gasto del ciclo desde el consejo
    Dado un ciclo en déficit con un gasto flexible propuesto
    Cuando presiono "Quitar del ciclo"
    Entonces aparece un aviso de que ya no cuenta
    Y el balance se recalcula

  @listo
  Escenario: Cuando no hay nada que mover se dice
    Dado un déficit donde todo lo demás es crítico
    Cuando abro el resumen
    Entonces se explica que no hay nada que mover sin tocar lo crítico

  @listo @critico
  Escenario: La barra muestra en qué se reparte el ingreso
    Dado un ciclo con gastos fijos, variables y ahorro
    Cuando abro el resumen
    Entonces la barra tiene un segmento por cada uno
    Y lo disponible es el espacio sin llenar

  @listo
  Escenario: Los gastos variables se marcan con trama
    Dado un ciclo con gastos sin confirmar
    Cuando miro la barra
    Entonces el segmento de los variables lleva trama diagonal

  @listo @critico
  Escenario: Con déficit la barra se pasa de la pista
    Dado un ciclo en déficit
    Cuando miro la barra
    Entonces hay una marca donde terminó el ingreso

  @listo @critico
  Escenario: Los pagos vencidos se listan primero
    Dado un ciclo con un pago vencido y dos por vencer
    Cuando abro el resumen
    Entonces el vencido aparece primero
    Y lleva su marca

  @listo
  Escenario: Sin pendientes se dice que no queda nada
    Dado un ciclo con todo pagado
    Cuando abro el resumen
    Entonces "Lo que sigue" muestra un estado vacío

  @listo @critico
  Escenario: Cerrar el ciclo pide confirmación y advierte qué se congela
    Cuando presiono "Cerrar este ciclo"
    Entonces el diálogo dice cuántos renglones quedan como registro
    Y advierte que ya no se podrán cambiar

  @listo @critico
  Escenario: La confirmación avisa si quedan renglones sin confirmar
    Dado un ciclo con tres renglones sin confirmar
    Cuando abro el diálogo de cierre
    Entonces dice que esos tres van a quedarse así

  @listo
  Escenario: Cancelar el cierre no cierra nada
    Dado que abrí el diálogo de cierre
    Cuando presiono Cancelar
    Entonces el ciclo sigue abierto

  @listo @critico
  Escenario: Con un solo ciclo no hay comparación
    Dado una cuenta con un solo ciclo
    Cuando abro el resumen
    Entonces la comparación muestra un estado vacío, no una gráfica

  @listo @critico
  Escenario: Con dos ciclos aparece la comparación y la gráfica
    Dado una cuenta con dos ciclos
    Cuando abro el resumen
    Entonces se dice cuánto más o menos se está gastando
    Y aparece la gráfica de balances

  @listo @responsive
  Escenario: En teléfono el resumen se lee en una columna
    Dado un viewport de teléfono
    Cuando abro el resumen
    Entonces no hay desplazamiento horizontal
```

---

## Sobre la gráfica

Una sola medida —el balance de cada ciclo— y un solo eje. No se grafican
ingresos y salidas como dos series: la pregunta es «voy mejor o peor», y eso lo
responde el balance. Dos series obligarían a restarlas con la vista.

**El color no es lo que dice el signo.** Lo dice la posición respecto al cero, y
lo confirma la etiqueta de cada barra. El verde y el rojo son refuerzo
convencional, y tienen que serlo: esa pareja falla la comprobación de daltonismo
con una separación de 3.3 en visión protan, muy por debajo del piso de 6. Quien
no distingue verde de rojo lee el signo igual — la barra apunta hacia abajo y el
número trae su menos.

Para la automatización: **no busques el color de la barra**. Busca la etiqueta y
el orden. El color es decorativo por diseño.

La paleta de las gráficas vive en `theme/tokens.ts` como `chartPalette`, con sus
valores verificados contra las cinco comprobaciones. El modo oscuro tiene pasos
propios, no es el claro aclarado.

---

## Cobertura automatizada hoy

| Prueba | Qué cubre |
|---|---|
| `DeficitAdvisorTest` | La decisión completa sin base de datos: qué nunca se propone, el orden entre grupos y dentro de cada uno, el desempate estable, cuándo se detiene y cuándo avisa que no alcanza |
| `BudgetDashboardIntegrationTest` | El armado del snapshot contra MySQL: que la flexibilidad y la prioridad lleguen de la base, y el orden y el tope de la comparación entre ciclos |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP y la interfaz,
que las pruebas de Java no ejercitan.
