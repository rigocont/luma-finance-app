# Ingresos

Capturar de dónde viene el dinero. Es la primera de las cuatro fases de captura
(ingresos, gastos fijos, gastos variables, ahorros) y la que libera al resto de
la suite de depender de filas insertadas por SQL.

**Estado:** API en la Fase 5a, pantalla en la 5b. Todo automatizable hoy.

---

## La regla que define esta fase

Un ingreso es una **plantilla**. El ciclo guarda **copias**. De ahí salen dos
comportamientos que parecen contradictorios y no lo son:

| Acción | Ciclo en curso | Ciclos siguientes |
|---|---|---|
| Capturar un ingreso nuevo | **Entra** si cae en el periodo | Entra |
| Editar un ingreso | **No cambia** | Aplica el cambio |
| Desactivar un ingreso | **No cambia** | No entra |
| Eliminar un ingreso | **No cambia** | No entra |

La asimetría es deliberada. Capturar el sueldo a media quincena y ver un
presupuesto sin ingresos no se le puede explicar a nadie. Pero si editar
reescribiera los renglones, lo que revisaste ayer podría ser otra cosa hoy, y un
ciclo dejaría de significar algo.

---

## Alta

```gherkin
# language: es

@api @ingresos
Característica: Capturar un ingreso
  Como persona que organiza su dinero
  Quiero registrar de dónde viene
  Para que el presupuesto sepa con qué cuento

  @listo @smoke @critico
  Escenario: Alta de un ingreso recurrente
    Cuando capturo un ingreso con nombre, tipo, monto, frecuencia y fecha de inicio
    Entonces la respuesta es 201
    Y el ingreso se identifica con un UUID
    Y queda activo
    Y el monto viaja como cadena con dos decimales

  @listo
  Escenario: Alta de un ingreso de una sola vez
    Cuando capturo un ingreso con frecuencia "ONE_TIME" y una fecha
    Entonces la respuesta es 201
    Y no se exige día del mes

  @listo
  Escenario: Alta con fecha de fin
    Cuando capturo un ingreso con fecha de fin posterior al inicio
    Entonces la respuesta es 201

  @listo @critico
  Escenario: El nombre es obligatorio
    Cuando capturo un ingreso sin nombre
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"
    Y el campo señalado es el nombre

  @listo
  Escenario: Un monto que no es un número se rechaza
    Cuando capturo un ingreso con un monto que es texto
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

  @listo
  Escenario: Un monto negativo se rechaza
    Cuando capturo un ingreso con un monto negativo
    Entonces la respuesta es 400

  @listo
  Escenario: Un monto con más de dos decimales se rechaza
    Cuando capturo un ingreso con un monto de tres decimales
    Entonces la respuesta es 400

  @listo
  Escenario: Un día fuera del rango 1 a 31 se rechaza
    Cuando capturo un ingreso con día esperado 32
    Entonces la respuesta es 400

  @listo
  Escenario: Una fecha de fin anterior al inicio se rechaza
    Cuando capturo un ingreso cuya fecha de fin es anterior a la de inicio
    Entonces la respuesta es 400

  @listo @seguridad
  Escenario: Capturar sin token se rechaza
    Dado que no estoy autenticado
    Cuando capturo un ingreso
    Entonces la respuesta es 401
```

---

## Relación con el ciclo abierto

El corazón de la fase. Estos escenarios cruzan dos módulos, así que solo se
pueden demostrar de extremo a extremo.

```gherkin
# language: es

@api @ingresos @presupuesto
Característica: Un ingreso nuevo y el ciclo en curso
  Como persona que captura su sueldo a media quincena
  Quiero verlo reflejado de inmediato
  Para no tener que esperar al siguiente periodo

  @listo @critico
  Escenario: Un ingreso nuevo entra al ciclo abierto
    Dado que tengo un ciclo abierto
    Cuando capturo un ingreso cuya fecha cae dentro del periodo
    Y consulto los renglones del ciclo
    Entonces aparece un renglón de ingreso con ese nombre y ese monto

  @listo
  Escenario: Un ingreso que no cae en el periodo no entra
    Dado que tengo un ciclo abierto
    Cuando capturo un ingreso anual cuyo mes ya pasó
    Entonces la respuesta es 201
    Y el ciclo no gana ningún renglón

  @listo @critico
  Escenario: Un ingreso de monto variable entra pidiendo revisión
    Dado que tengo un ciclo abierto
    Cuando capturo un ingreso de tipo "VARIABLE"
    Entonces su renglón queda en estado "NEEDS_REVIEW"

  @listo
  Escenario: Un ingreso de monto estable entra como pendiente
    Dado que tengo un ciclo abierto
    Cuando capturo un ingreso de tipo "RECURRENT"
    Entonces su renglón queda en estado "PENDING"

  @listo @critico
  Escenario: Editar el monto no toca el renglón ya generado
    Dado que tengo un ciclo abierto con un ingreso de 12,500
    Cuando cambio el monto del ingreso a 20,000
    Y consulto los renglones del ciclo
    Entonces el renglón sigue valiendo 12,500

  @listo @critico
  Escenario: Eliminar un ingreso no quita el renglón ya generado
    Dado que tengo un ciclo abierto con un ingreso
    Cuando elimino el ingreso
    Entonces el renglón sigue en el ciclo

  @listo
  Escenario: Capturar sin ciclo abierto no falla
    Dado que no tengo ningún ciclo
    Cuando capturo un ingreso
    Entonces la respuesta es 201
    Y no se crea ningún ciclo

  @listo
  Escenario: El ingreso capturado antes aparece al abrir el ciclo
    Dado que capturé un ingreso sin tener ciclo abierto
    Cuando abro el siguiente ciclo
    Entonces su renglón aparece en el ciclo nuevo

  @listo
  Escenario: Un ingreso desactivado no entra en el siguiente ciclo
    Dado que tengo un ingreso desactivado
    Cuando abro el siguiente ciclo
    Entonces no aparece ningún renglón suyo
```

> El escenario de editar es el menos intuitivo y el más importante. Es lo que
> sostiene que el historial sea confiable.

---

## Consulta y listado

```gherkin
# language: es

@api @ingresos
Característica: Listar ingresos
  Como persona con varias entradas de dinero
  Quiero encontrarlas rápido
  Para revisarlas y corregirlas

  @listo @smoke
  Escenario: Listar mis ingresos
    Dado que tengo varios ingresos
    Cuando consulto la lista
    Entonces la respuesta es 200
    Y viene paginada con total de elementos y de páginas

  @listo
  Escenario: El orden por omisión es el más reciente primero
    Cuando consulto la lista sin indicar orden
    Entonces el ingreso capturado más recientemente aparece primero

  @listo
  Escenario: Ordenar por monto
    Cuando consulto la lista ordenando por "AMOUNT_DESC"
    Entonces el ingreso de mayor monto aparece primero

  @listo @critico
  Escenario: Un orden que no existe se rechaza
    Cuando consulto la lista pidiendo un orden inventado
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

  @listo
  Escenario: Filtrar por tipo
    Dado que tengo ingresos de varios tipos
    Cuando filtro por tipo "VARIABLE"
    Entonces todos los devueltos son de ese tipo

  @listo
  Escenario: Filtrar por activos
    Dado que tengo ingresos activos y desactivados
    Cuando filtro por activos
    Entonces no aparece ninguno desactivado

  @listo
  Escenario: El tamaño de página tiene tope
    Cuando pido una página de mil elementos
    Entonces la respuesta es 200
    Y no devuelve más de cien

  @listo
  Escenario: Consultar un ingreso por su identificador
    Cuando consulto un ingreso por su UUID
    Entonces la respuesta es 200
    Y el cuerpo indica si su monto pedirá revisión

  @listo
  Escenario: Un ingreso que no existe da 404
    Cuando consulto un ingreso con un identificador inventado
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"
```

---

## Edición, desactivación y borrado

```gherkin
# language: es

@api @ingresos
Característica: Mantener un ingreso
  Como persona cuyo sueldo cambia
  Quiero corregir lo que capturé
  Para que el presupuesto siga siendo cierto

  @listo @critico
  Escenario: Cambiar el monto
    Cuando cambio el monto de un ingreso
    Entonces la respuesta es 200
    Y el ingreso queda con el monto nuevo

  @listo
  Escenario: Lo que no mando no se toca
    Cuando cambio solo el nombre de un ingreso
    Entonces el monto, la frecuencia y las fechas quedan igual

  @listo
  Escenario: Cambiar el calendario completo
    Cuando cambio la frecuencia, el día y las fechas a la vez
    Entonces la respuesta es 200
    Y el ingreso queda con el calendario nuevo

  @listo
  Escenario: Un calendario inválido no deja el ingreso a medias
    Cuando intento cambiar el calendario con una fecha de fin anterior al inicio
    Entonces la respuesta es 400
    Y el ingreso conserva su calendario anterior

  @listo
  Escenario: Desactivar un ingreso
    Cuando desactivo un ingreso
    Entonces la respuesta es 200
    Y queda inactivo
    Y sigue apareciendo en la lista

  @listo
  Escenario: Reactivar un ingreso desactivado
    Dado que tengo un ingreso desactivado
    Cuando lo reactivo
    Entonces queda activo

  @listo @critico
  Escenario: Eliminar un ingreso lo saca de la lista
    Cuando elimino un ingreso
    Entonces la respuesta es 204
    Y ya no aparece en la lista
    Y consultarlo por su identificador da 404

  @listo @critico
  Escenario: Un ingreso eliminado no se puede reactivar
    Dado que eliminé un ingreso
    Cuando intento reactivarlo
    Entonces la respuesta es 404

  @listo
  Escenario: Eliminar dos veces da 404 la segunda
    Dado que eliminé un ingreso
    Cuando lo elimino otra vez
    Entonces la respuesta es 404
```

---

## Aislamiento entre usuarios

```gherkin
# language: es

@api @ingresos @seguridad
Característica: Cada quien ve solo sus ingresos

  @listo @critico @seguridad
  Escenario: No se ven los ingresos de otra persona
    Dado que existe un ingreso de otro usuario
    Cuando consulto mi lista
    Entonces ese ingreso no aparece

  @listo @critico @seguridad
  Escenario: El ingreso de otra persona responde 404
    Dado que existe un ingreso de otro usuario
    Cuando lo consulto por su identificador
    Entonces la respuesta es 404
    Y no 403: la respuesta no revela que exista

  @listo @seguridad
  Escenario: El ingreso de otra persona no se puede editar ni eliminar
    Dado que existe un ingreso de otro usuario
    Cuando intento editarlo
    Entonces la respuesta es 404
    Cuando intento eliminarlo
    Entonces la respuesta es 404

  @listo @seguridad
  Escenario: Ninguna ruta de ingresos acepta un identificador de usuario
    Cuando reviso la especificación OpenAPI del módulo de ingresos
    Entonces ninguna ruta incluye un identificador de usuario
```

---

## La pantalla

```gherkin
# language: es

@ui @ingresos
Característica: Pantalla de ingresos
  Como persona que acaba de crear su cuenta
  Quiero capturar de dónde viene mi dinero
  Para que el presupuesto deje de estar vacío

  @listo @smoke @critico
  Escenario: La lista vacía invita a capturar el primero
    Dado que no tengo ingresos
    Cuando entro a Ingresos
    Entonces veo un estado vacío
    Y un botón para capturar el primero

  @listo @smoke @critico
  Escenario: Capturar un ingreso desde el cajón
    Cuando presiono "Capturar ingreso"
    Entonces se abre el cajón del formulario
    Cuando lleno nombre, tipo, monto, frecuencia y día
    Y guardo
    Entonces el cajón se cierra
    Y aparece un aviso de que el ingreso quedó capturado
    Y el ingreso aparece en la tabla

  @listo @critico
  Escenario: Elegir un tipo de monto variable avisa que pedirá confirmación
    Dado que abrí el formulario
    Cuando elijo el tipo "Monto variable"
    Entonces aparece un aviso explicando que cada ciclo pedirá confirmar el monto

  @listo
  Escenario: Las frecuencias anual y de una sola vez no piden día del mes
    Dado que abrí el formulario
    Cuando elijo la frecuencia "Una vez al año"
    Entonces el campo de día del mes desaparece

  @listo @critico
  Escenario: Un error de validación se muestra junto a su campo
    Dado que abrí el formulario
    Cuando guardo con el monto en blanco
    Entonces el cajón sigue abierto
    Y el mensaje aparece debajo del campo de monto
    Y no se pierde lo que ya había capturado

  @listo
  Escenario: Cancelar cierra el cajón sin guardar
    Dado que abrí el formulario y escribí un nombre
    Cuando presiono Cancelar
    Entonces el cajón se cierra
    Y la tabla no cambia

  @listo @critico
  Escenario: Editar un ingreso desde la lista
    Dado que tengo un ingreso en la tabla
    Cuando abro su menú y elijo Editar
    Entonces el cajón se abre con sus datos
    Y el cajón advierte que el cambio aplica desde el siguiente ciclo

  @listo @critico
  Escenario: Eliminar pide confirmación
    Cuando abro el menú de un ingreso y elijo Eliminar
    Entonces aparece un diálogo con el nombre del ingreso
    Y el diálogo advierte que no se puede recuperar
    Y ofrece "Dejar de contarlo" como alternativa

  @listo @critico
  Escenario: Cancelar la confirmación no elimina nada
    Dado que abrí el diálogo de eliminación
    Cuando presiono Cancelar
    Entonces el ingreso sigue en la tabla

  @listo
  Escenario: Confirmar la eliminación lo saca de la tabla
    Dado que abrí el diálogo de eliminación
    Cuando confirmo
    Entonces el ingreso desaparece de la tabla
    Y aparece un aviso de que quedó eliminado

  @listo
  Escenario: Dejar de contar un ingreso lo marca sin quitarlo
    Cuando abro el menú de un ingreso y elijo "Dejar de contarlo"
    Entonces la fila se marca como "Sin contar"
    Y sigue en la tabla

  @listo
  Escenario: Un ingreso sin contar se puede volver a contar
    Dado que tengo un ingreso marcado como "Sin contar"
    Cuando abro su menú y elijo "Volver a contarlo"
    Entonces la marca desaparece

  @listo
  Escenario: Un ingreso de monto variable se señala en la tabla
    Dado que tengo un ingreso de tipo variable
    Cuando veo la tabla
    Entonces su fila muestra la marca "Pide revisión"

  @listo
  Escenario: Filtrar por tipo vuelve a la primera página
    Dado que estoy en la segunda página
    Cuando filtro por un tipo
    Entonces vuelvo a la primera página

  @listo
  Escenario: Con filtros y sin resultados se ofrece quitarlos
    Cuando filtro por un tipo del que no tengo ingresos
    Entonces el estado vacío ofrece quitar los filtros

  @listo
  Escenario: Los montos se muestran en formato es-MX
    Entonces un monto de 12500 se muestra como $12,500.00

  @listo
  Escenario: La lista muestra estado de carga
    Cuando entro a Ingresos
    Entonces veo esqueletos mientras llegan los datos

  @listo
  Escenario: Un fallo de la API muestra el estado de error con reintento
    Dado que la API responde con error
    Cuando entro a Ingresos
    Entonces veo el estado de error
    Y un botón para reintentar

  @listo @responsive
  Escenario: En teléfono el cajón ocupa el ancho completo
    Dado un viewport de teléfono
    Cuando abro el formulario
    Entonces el cajón ocupa todo el ancho
```

## Cobertura automatizada hoy

Lo que ya verifica la suite del proyecto, para no duplicar esfuerzo al
automatizar:

| Prueba | Qué cubre |
|---|---|
| `IncomeTest` (20) | Validaciones puras, regla de revisión por tipo, activar/desactivar/eliminar |
| `IncomeIntegrationTest` (12) | La relación con el ciclo abierto, filtros, borrado lógico y aislamiento entre usuarios |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP —códigos,
formato de error, paginación— que las pruebas de Java no ejercitan.
