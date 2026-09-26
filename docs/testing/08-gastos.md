# Gastos

Lo que sale cada ciclo. Fijos y variables comparten tabla, API y pantalla: solo
los distingue un campo.

**Estado:** API y pantalla en la Fase 6. Todo automatizable hoy.

---

## Por qué una sola sección y no dos

La Fase 0 ya lo había decidido para el modelo de datos: separar fijos de
variables en dos mecanismos duplicaba el cálculo sin resolver nada. La misma
razón aplica a la interfaz — dos pantallas idénticas salvo por un booleano.

La diferencia real no está en cómo se guardan sino en **cómo se materializan**:

| | Gasto fijo | Gasto variable |
|---|---|---|
| Estado al entrar al ciclo | `PENDING` | `NEEDS_REVIEW` |
| El balance lo da por seguro | Sí | No, hasta que confirmes |

La Fase 7 no es «la pantalla de gastos variables»: es la de **revisión por
ciclo**, donde se confirma cuánto fue de verdad cada uno.

---

## El catálogo de categorías

```gherkin
# language: es

@api @gastos @categorias
Característica: Catálogo de categorías
  Como persona que captura sus gastos
  Quiero elegir de una lista ya armada
  Para no inventar una taxonomía desde cero

  @listo @smoke
  Escenario: El catálogo trae las categorías del sistema
    Cuando consulto el catálogo de categorías
    Entonces la respuesta es 200
    Y trae 17 categorías
    Y todas están marcadas como del sistema

  @listo
  Escenario: El catálogo viene en el orden en que debe mostrarse
    Cuando consulto el catálogo
    Entonces "Vivienda" aparece primero
    Y "Otros" aparece al final

  @listo
  Escenario: Cada categoría sugiere un tipo de monto
    Cuando consulto el catálogo
    Entonces "Vivienda" sugiere monto estable
    Y "Despensa" sugiere monto que cambia

  @listo @seguridad
  Escenario: El catálogo exige sesión
    Dado que no estoy autenticado
    Cuando consulto el catálogo
    Entonces la respuesta es 401
```

---

## Alta

```gherkin
# language: es

@api @gastos
Característica: Capturar un gasto
  Como persona que quiere saber cuánto le queda
  Quiero registrar lo que se me va cada ciclo
  Para que el balance signifique algo

  @listo @smoke @critico
  Escenario: Alta de un gasto fijo
    Cuando capturo un gasto con nombre, monto estable, flexibilidad, frecuencia y día
    Entonces la respuesta es 201
    Y el gasto se identifica con un UUID
    Y queda activo

  @listo @critico
  Escenario: Alta de un gasto variable
    Cuando capturo un gasto indicando que el monto cambia
    Entonces la respuesta es 201
    Y el cuerpo indica que pedirá revisión

  @listo
  Escenario: Un gasto sin categoría es válido
    Cuando capturo un gasto sin indicar categoría
    Entonces la respuesta es 201
    Y el gasto queda sin categoría

  @listo @critico
  Escenario: Una categoría inventada se rechaza
    Cuando capturo un gasto con un identificador de categoría que no existe
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"

  @listo @critico @seguridad
  Escenario: No se puede usar la categoría propia de otra persona
    Dado que otro usuario creó una categoría propia
    Cuando capturo un gasto con esa categoría
    Entonces la respuesta es 404

  @listo @critico
  Escenario: La flexibilidad es obligatoria
    Cuando capturo un gasto sin indicar qué tanto se puede mover
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

  @listo
  Escenario: El nombre es obligatorio
    Cuando capturo un gasto sin nombre
    Entonces la respuesta es 400

  @listo
  Escenario: Un monto negativo se rechaza
    Cuando capturo un gasto con monto negativo
    Entonces la respuesta es 400

  @listo
  Escenario: Una fecha de fin anterior al inicio se rechaza
    Cuando capturo un gasto cuya fecha de fin es anterior a la de inicio
    Entonces la respuesta es 400
```

---

## Relación con el ciclo abierto

Misma regla que en ingresos, y por la misma razón.

```gherkin
# language: es

@api @gastos @presupuesto
Característica: Un gasto nuevo y el ciclo en curso

  @listo @critico
  Escenario: Un gasto nuevo entra al ciclo abierto
    Dado que tengo un ciclo abierto
    Cuando capturo un gasto cuya fecha cae dentro del periodo
    Y consulto los renglones del ciclo
    Entonces aparece un renglón con ese nombre y ese monto

  @listo @critico
  Escenario: Un gasto fijo entra como pendiente
    Dado que tengo un ciclo abierto
    Cuando capturo un gasto de monto estable
    Entonces su renglón es de tipo "FIXED_EXPENSE"
    Y queda en estado "PENDING"

  @listo @critico
  Escenario: Un gasto variable entra pidiendo revisión
    Dado que tengo un ciclo abierto
    Cuando capturo un gasto de monto que cambia
    Entonces su renglón es de tipo "VARIABLE_EXPENSE"
    Y queda en estado "NEEDS_REVIEW"

  @listo @critico
  Escenario: La flexibilidad se copia al renglón
    Dado que tengo un ciclo abierto
    Cuando capturo un gasto marcado como crítico
    Entonces su renglón del ciclo también queda marcado como crítico

  @listo @critico
  Escenario: Editar el monto no toca el renglón ya generado
    Dado que tengo un ciclo abierto con un gasto de 6,000
    Cuando cambio el monto del gasto a 9,000
    Entonces el renglón del ciclo sigue valiendo 6,000

  @listo
  Escenario: Un gasto desactivado no entra en el siguiente ciclo
    Dado que tengo un gasto desactivado
    Cuando abro el siguiente ciclo
    Entonces no aparece ningún renglón suyo
```

> El renglón guarda su propia copia de la flexibilidad en lugar de consultarla
> en la plantilla. Es deliberado: el módulo de análisis mira el ciclo, y la
> plantilla pudo haber cambiado después de cerrarlo.

---

## Listado, edición y borrado

```gherkin
# language: es

@api @gastos
Característica: Mantener los gastos

  @listo @smoke
  Escenario: Listar mis gastos
    Cuando consulto la lista
    Entonces la respuesta es 200
    Y viene paginada

  @listo @critico
  Escenario: Filtrar por tipo de monto separa fijos de variables
    Dado que tengo gastos de los dos tipos
    Cuando filtro por monto estable
    Entonces no aparece ninguno de monto variable

  @listo
  Escenario: Filtrar por categoría
    Dado que tengo gastos en varias categorías
    Cuando filtro por una de ellas
    Entonces todos los devueltos son de esa categoría

  @listo
  Escenario: Ordenar por día de pago
    Cuando consulto la lista ordenando por día de pago
    Entonces el que vence antes aparece primero

  @listo
  Escenario: Un orden que no existe se rechaza
    Cuando consulto la lista pidiendo un orden inventado
    Entonces la respuesta es 400

  @listo
  Escenario: Cambiar de fijo a variable cambia cómo se materializa
    Dado que tengo un gasto de monto estable
    Cuando lo cambio a monto que cambia
    Y abro el siguiente ciclo
    Entonces su renglón pide revisión

  @listo
  Escenario: Quitarle la categoría a un gasto es válido
    Dado que tengo un gasto con categoría
    Cuando lo edito quitándole la categoría
    Entonces la respuesta es 200
    Y el gasto queda sin categoría

  @listo
  Escenario: Un calendario inválido no deja el gasto a medias
    Cuando intento cambiar el calendario con una fecha de fin anterior al inicio
    Entonces la respuesta es 400
    Y el gasto conserva su calendario anterior

  @listo
  Escenario: Desactivar un gasto lo conserva
    Cuando desactivo un gasto
    Entonces queda inactivo
    Y sigue apareciendo en la lista

  @listo @critico
  Escenario: Eliminar un gasto lo saca de la lista
    Cuando elimino un gasto
    Entonces la respuesta es 204
    Y consultarlo por su identificador da 404

  @listo @critico
  Escenario: Un gasto eliminado no se puede reactivar
    Dado que eliminé un gasto
    Cuando intento reactivarlo
    Entonces la respuesta es 404

  @listo @critico @seguridad
  Escenario: No se ven los gastos de otra persona
    Dado que existe un gasto de otro usuario
    Cuando consulto mi lista
    Entonces ese gasto no aparece
    Y consultarlo por su identificador da 404
```

---

## La pantalla

```gherkin
# language: es

@ui @gastos
Característica: Pantalla de gastos
  Como persona organizando su presupuesto
  Quiero capturar y revisar lo que me sale cada ciclo
  Para saber con qué me quedo

  @listo @smoke @critico
  Escenario: La lista vacía invita a capturar el primero
    Dado que no tengo gastos
    Cuando entro a Gastos
    Entonces veo un estado vacío
    Y sugiere empezar por renta, servicios y despensa

  @listo @smoke @critico
  Escenario: Capturar un gasto desde el cajón
    Cuando presiono "Capturar gasto"
    Y lleno nombre, monto, flexibilidad, frecuencia y día
    Y guardo
    Entonces el cajón se cierra
    Y aparece un aviso de que el gasto quedó capturado
    Y el gasto aparece en la tabla

  @listo @critico
  Escenario: Elegir categoría sugiere el tipo de monto
    Dado que abrí el formulario para capturar uno nuevo
    Cuando elijo la categoría "Despensa"
    Entonces el tipo de monto cambia a "Monto que cambia"

  @listo
  Escenario: Al editar, elegir categoría NO cambia el tipo de monto
    Dado que estoy editando un gasto de monto estable
    Cuando cambio su categoría a "Despensa"
    Entonces el tipo de monto sigue siendo estable

  @listo @critico
  Escenario: Elegir monto que cambia avisa que pedirá confirmación
    Dado que abrí el formulario
    Cuando elijo "Monto que cambia"
    Entonces aparece un aviso de que cada ciclo pedirá confirmar cuánto fue
    Y la etiqueta del campo pasa a "Monto estimado"

  @listo @critico
  Escenario: Marcar un gasto como crítico avisa qué significa
    Dado que abrí el formulario
    Cuando elijo "No se puede mover"
    Entonces aparece un aviso de que LUMA nunca sugerirá retrasar ese pago

  @listo
  Escenario: Las frecuencias anual y de una sola vez no piden día de pago
    Dado que abrí el formulario
    Cuando elijo la frecuencia "Una vez al año"
    Entonces el campo de día de pago desaparece

  @listo @critico
  Escenario: Un error de validación se muestra junto a su campo
    Dado que abrí el formulario
    Cuando guardo con el monto en blanco
    Entonces el cajón sigue abierto
    Y el mensaje aparece debajo del campo de monto
    Y no se pierde lo que ya había capturado

  @listo
  Escenario: La tabla señala los gastos críticos
    Dado que tengo un gasto marcado como crítico
    Cuando veo la tabla
    Entonces su fila lleva una marca "Critico" destacada

  @listo
  Escenario: La tabla señala los que piden revisión
    Dado que tengo un gasto de monto que cambia
    Entonces su fila lleva la marca "Pide revisión"

  @listo
  Escenario: Un gasto sin categoría lo dice
    Dado que tengo un gasto sin categoría
    Entonces su fila muestra "Sin categoria"

  @listo @critico
  Escenario: Eliminar pide confirmación
    Cuando abro el menú de un gasto y elijo Eliminar
    Entonces aparece un diálogo con el nombre del gasto
    Y ofrece "Dejar de contarlo" como alternativa

  @listo @critico
  Escenario: Cancelar la confirmación no elimina nada
    Dado que abrí el diálogo de eliminación
    Cuando presiono Cancelar
    Entonces el gasto sigue en la tabla

  @listo
  Escenario: Filtrar por categoría usa el catálogo real
    Cuando abro el filtro de categoría
    Entonces veo las 17 categorías del sistema

  @listo
  Escenario: Con filtros y sin resultados se ofrece quitarlos
    Cuando filtro por una combinación sin gastos
    Entonces el estado vacío ofrece quitar los filtros

  @listo @responsive
  Escenario: En teléfono el cajón ocupa el ancho completo
    Dado un viewport de teléfono
    Cuando abro el formulario
    Entonces el cajón ocupa todo el ancho
```

---

## Cobertura automatizada hoy

| Prueba | Qué cubre |
|---|---|
| `ExpenseTest` | Validaciones puras, clasificación fijo/variable, flexibilidad, ciclo de vida |
| `ExpenseIntegrationTest` | Catálogo sembrado, relación con el ciclo abierto, filtros, borrado lógico y aislamiento entre usuarios |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP y la interfaz,
que las pruebas de Java no ejercitan.
