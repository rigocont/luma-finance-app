# Ciclos presupuestales

El corazón del producto: convertir la configuración de una persona en un plan con
fechas y montos, y decir si le alcanza.

Todo se ejercita por HTTP. **Todavía no hay interfaz para esto** — la pantalla del
ciclo llega en la Fase 10 y los escenarios `@ui` se agregan entonces.

**Estado:** implementado en la Fase 4b.

---

## Antes de empezar: las plantillas no tienen API todavía

Un ciclo se materializa a partir de los ingresos, gastos y metas del usuario. El
CRUD de esas tres cosas llega en las Fases 5 a 8, así que hoy **no hay forma de
crearlos por la API**.

Consecuencia práctica: abrir un ciclo en una cuenta nueva devuelve un ciclo
correcto con **cero renglones** y balance en cero. Eso no es un defecto, es el
estado real del sistema.

Los escenarios marcados `@requiere-semilla` necesitan filas insertadas
directamente en `incomes`, `expenses` o `savings_goals`. Cuando exista el CRUD,
la etiqueta desaparece y el `Dado` se reescribe como llamadas a la API — el
`Cuando` y el `Entonces` no cambian.

```sql
-- Semilla mínima para ejercitar la materialización.
-- Sustituye el correo por el del usuario de prueba.
SET @uid = (SELECT id FROM users WHERE email = 'qa@luma.app');

INSERT INTO incomes (public_id, user_id, name, income_type, amount, frequency, expected_day, start_date, active)
VALUES (UUID(), @uid, 'Sueldo', 'RECURRENT', 12500.00, 'BIWEEKLY', 15, '2026-01-01', TRUE);

INSERT INTO expenses (public_id, user_id, name, expense_kind, flexibility, amount, frequency, due_day, start_date, active)
VALUES (UUID(), @uid, 'Renta',    'FIXED',    'CRITICAL',  6000.00, 'MONTHLY', 1, '2026-01-01', TRUE),
       (UUID(), @uid, 'Despensa', 'VARIABLE', 'IMPORTANT', 2500.00, 'MONTHLY', 5, '2026-01-01', TRUE);

INSERT INTO savings_goals (public_id, user_id, name, target_amount, current_amount, contribution_mode, planned_per_cycle, priority, status)
VALUES (UUID(), @uid, 'Fondo de emergencia', 30000.00, 0.00, 'FIXED_PER_CYCLE', 1000.00, 1, 'ACTIVE');
```

Con esa semilla, un ciclo quincenal debe materializar un ingreso de 12,500, la
renta o la despensa según la quincena, y un ahorro de 1,000.

---

## Abrir un ciclo

```gherkin
# language: es

@api @presupuesto @ciclos
Característica: Abrir un ciclo presupuestal
  Como persona que organiza su dinero por periodos
  Quiero abrir el ciclo en curso
  Para ver mi plan con fechas y montos concretos

  @listo @smoke @critico
  Escenario: Abrir el primer ciclo de una cuenta nueva
    Dado que estoy autenticado y no tengo ningún ciclo
    Cuando abro el siguiente ciclo
    Entonces la respuesta es 201
    Y el ciclo tiene el número de secuencia 1
    Y el estado del ciclo es "ACTIVE"
    Y el periodo corresponde al tipo de ciclo de mis preferencias
    Y el cuerpo incluye el balance del ciclo

  @listo
  Escenario: Una cuenta sin ingresos ni gastos abre un ciclo vacío
    Dado que estoy autenticado y no tengo ingresos, gastos ni metas
    Cuando abro el siguiente ciclo
    Entonces la respuesta es 201
    Y el ciclo no tiene renglones
    Y el balance planeado es cero
    Y el estado del presupuesto es "BALANCED"

  @listo @critico
  Escenario: No se puede abrir el siguiente ciclo si el actual no ha terminado
    Dado que tengo un ciclo activo cuyo periodo todavía no termina
    Cuando abro el siguiente ciclo
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"
    Y el mensaje explica que debo cerrar el ciclo en curso

  @listo
  Escenario: El ciclo vencido se cierra solo al abrir el siguiente
    Dado que tengo un ciclo activo cuyo periodo ya terminó
    Cuando abro el siguiente ciclo
    Entonces la respuesta es 201
    Y el ciclo anterior queda en estado "CLOSED"
    Y el nuevo ciclo empieza el día siguiente al fin del anterior
    Y su número de secuencia es el del anterior más uno

  @listo @seguridad
  Escenario: Abrir un ciclo sin token se rechaza
    Dado que no estoy autenticado
    Cuando abro el siguiente ciclo
    Entonces la respuesta es 401
    Y el código de error es "UNAUTHORIZED"
```

---

## Materialización de renglones

Lo que ocurre al abrir el ciclo: las plantillas se convierten en renglones con
fecha. **Cada renglón es una copia**, no una referencia — desde ese momento el
ciclo tiene vida propia.

```gherkin
# language: es

@api @presupuesto @ciclos
Característica: Materialización de un ciclo
  Como persona con ingresos y gastos configurados
  Quiero que el ciclo se genere solo
  Para no capturar lo mismo cada periodo

  @listo @critico @requiere-semilla
  Escenario: Abrir el ciclo materializa ingresos, gastos y metas
    Dado que tengo un ingreso, un gasto fijo, un gasto variable y una meta activos
    Cuando abro el siguiente ciclo
    Entonces hay un renglón de tipo "INCOME" por cada ocurrencia del ingreso
    Y un renglón de tipo "FIXED_EXPENSE" por cada ocurrencia del gasto fijo
    Y un renglón de tipo "VARIABLE_EXPENSE" por cada ocurrencia del gasto variable
    Y un renglón de tipo "SAVING" por la meta
    Y cada renglón lleva su propia fecha de vencimiento

  @listo @critico @requiere-semilla
  Escenario: Los gastos variables nacen pendientes de revisión
    Dado que tengo un gasto variable activo
    Cuando abro el siguiente ciclo
    Entonces el renglón del gasto variable tiene estado "NEEDS_REVIEW"

  @listo @requiere-semilla
  Escenario: Los gastos fijos nacen pendientes
    Dado que tengo un gasto fijo activo
    Cuando abro el siguiente ciclo
    Entonces el renglón del gasto fijo tiene estado "PENDING"

  @listo @requiere-semilla
  Escenario: Un ingreso quincenal produce dos renglones en un ciclo mensual
    Dado que mi tipo de ciclo es mensual
    Y tengo un ingreso con frecuencia quincenal
    Cuando abro el siguiente ciclo
    Entonces hay dos renglones de ingreso
    Y sus fechas están separadas por quince días

  @listo @requiere-semilla
  Escenario: Un gasto mensual produce dos renglones en un ciclo bimestral
    Dado que mi tipo de ciclo es bimestral
    Y tengo un gasto fijo con frecuencia mensual
    Cuando abro el siguiente ciclo
    Entonces hay dos renglones de ese gasto
    Y cada uno cae en un mes distinto del periodo

  @listo @requiere-semilla
  Escenario: Un gasto que vence el 31 en un mes de 30 días cae el último día
    Dado que tengo un gasto fijo que vence el día 31
    Cuando abro un ciclo que cubre un mes de 30 días
    Entonces el renglón vence el día 30

  @listo @requiere-semilla
  Escenario: Febrero en año bisiesto
    Dado que tengo un gasto fijo que vence el día 30
    Cuando abro un ciclo que cubre febrero de un año bisiesto
    Entonces el renglón vence el día 29

  @listo @requiere-semilla
  Escenario: Un ingreso inactivo no se materializa
    Dado que tengo un ingreso desactivado
    Cuando abro el siguiente ciclo
    Entonces no hay ningún renglón para ese ingreso

  @listo @requiere-semilla
  Escenario: Una meta con aporte calculado en cero no se materializa
    Dado que tengo una meta sin fecha objetivo y sin aporte fijo
    Cuando abro el siguiente ciclo
    Entonces no hay ningún renglón para esa meta

  @listo @requiere-semilla
  Escenario: El aporte a la meta se espera al cierre del ciclo
    Dado que tengo una meta activa con aporte por ciclo
    Cuando abro el siguiente ciclo
    Entonces el renglón de ahorro vence el último día del periodo

  @listo @critico
  Escenario: La política de prorrateo no está disponible todavía
    Dado que mi política de asignación de gastos es "PRORATE"
    Cuando abro el siguiente ciclo
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"
    Y el mensaje me dice que cambie la preferencia a asignación por fecha de vencimiento
```

> El último escenario es deliberado: el motor **falla en voz alta** en lugar de
> calcular con una regla distinta de la que la persona eligió. Un presupuesto
> silenciosamente equivocado es peor que un error claro.

---

## El ciclo en curso y el balance

```gherkin
# language: es

@api @presupuesto @balance
Característica: Balance del ciclo
  Como persona que quiere saber si le alcanza
  Quiero consultar el balance de mi ciclo
  Para decidir con cifras, no con intuición

  @listo @smoke @critico
  Escenario: Consultar el ciclo en curso
    Dado que tengo un ciclo activo
    Cuando consulto el ciclo en curso
    Entonces la respuesta es 200
    Y el cuerpo incluye el ciclo, su periodo y su balance

  @listo
  Escenario: Sin ningún ciclo abierto no hay ciclo en curso
    Dado que estoy autenticado y no tengo ningún ciclo
    Cuando consulto el ciclo en curso
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"

  @listo @critico @requiere-semilla
  Escenario: El balance distingue lo planeado de lo real
    Dado que tengo un ciclo con renglones y ninguno confirmado
    Cuando consulto el balance del ciclo
    Entonces los totales planeados reflejan los montos materializados
    Y los totales reales están en cero

  @listo @critico @requiere-semilla
  Escenario: Te alcanza cuando los ingresos superan las salidas
    Dado que mis ingresos planeados superan mis gastos y ahorros planeados
    Cuando consulto el balance del ciclo
    Entonces el estado del presupuesto es "SURPLUS"
    Y el balance planeado es positivo

  @listo @critico @requiere-semilla
  Escenario: Vas justo cuando ingresos y salidas coinciden
    Dado que mis ingresos planeados igualan exactamente mis salidas planeadas
    Cuando consulto el balance del ciclo
    Entonces el estado del presupuesto es "BALANCED"
    Y el balance planeado es cero

  @listo @critico @requiere-semilla
  Escenario: Te falta cuando las salidas superan los ingresos
    Dado que mis salidas planeadas superan mis ingresos planeados
    Cuando consulto el balance del ciclo
    Entonces el estado del presupuesto es "DEFICIT"
    Y el balance planeado es negativo

  @listo @requiere-semilla
  Escenario: Un renglón omitido no entra en el cálculo
    Dado que tengo un ciclo con un gasto planeado
    Cuando quito ese renglón del ciclo
    Y consulto el balance del ciclo
    Entonces el gasto ya no aparece en los totales planeados

  @listo @critico
  Escenario: Los importes conservan dos decimales exactos
    Cuando consulto el balance del ciclo
    Entonces cada importe viaja como cadena con exactamente dos decimales
    Y ningún importe viaja como número de punto flotante

  @listo @requiere-semilla
  Escenario: La suma de los renglones cuadra con el total
    Dado que tengo un ciclo con varios gastos planeados
    Cuando consulto el balance del ciclo
    Entonces el total de gastos planeados es la suma exacta de sus renglones

  @listo @requiere-semilla
  Escenario: Las tasas se calculan sobre el ingreso
    Dado que tengo un ciclo con ingresos y ahorros planeados
    Cuando consulto el balance del ciclo
    Entonces el cuerpo incluye la proporción destinada a ahorro
    Y la proporción destinada a gastos

  @listo
  Escenario: Sin ingresos las tasas son cero y no hay división por cero
    Dado que tengo un ciclo sin ningún ingreso planeado
    Cuando consulto el balance del ciclo
    Entonces la respuesta es 200
    Y las proporciones son cero

  @listo @requiere-semilla
  Escenario: El conteo de renglones se reporta por estado
    Dado que tengo un ciclo con renglones en varios estados
    Cuando consulto el ciclo en curso
    Entonces el cuerpo incluye cuántos renglones hay pendientes, pagados, vencidos y por revisar
```

---

## Renglones del ciclo

```gherkin
# language: es

@api @presupuesto @renglones
Característica: Consultar los renglones de un ciclo
  Como persona que revisa su plan
  Quiero listar y filtrar los renglones
  Para ver qué viene y qué falta

  @listo @smoke @requiere-semilla
  Escenario: Listar los renglones de un ciclo
    Dado que tengo un ciclo con renglones
    Cuando consulto los renglones del ciclo
    Entonces la respuesta es 200
    Y vienen ordenados por fecha de vencimiento

  @listo @requiere-semilla
  Escenario: Filtrar los renglones por tipo
    Dado que tengo un ciclo con ingresos y gastos
    Cuando consulto los renglones del ciclo filtrando por tipo "FIXED_EXPENSE"
    Entonces todos los renglones devueltos son gastos fijos

  @listo @requiere-semilla
  Escenario: Filtrar los renglones por estado
    Dado que tengo un ciclo con renglones pendientes y por revisar
    Cuando consulto los renglones del ciclo filtrando por estado "NEEDS_REVIEW"
    Entonces todos los renglones devueltos están por revisar

  @listo
  Escenario: Un tipo inválido se rechaza
    Cuando consulto los renglones del ciclo filtrando por un tipo que no existe
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

  @listo
  Escenario: Un renglón nunca expone el identificador interno
    Cuando consulto los renglones del ciclo
    Entonces cada renglón se identifica con un UUID
    Y ningún renglón expone un identificador numérico secuencial
```

---

## Ajustar y confirmar renglones

```gherkin
# language: es

@api @presupuesto @renglones
Característica: Ajustar un renglón del ciclo
  Como persona cuyo gasto real no siempre coincide con el plan
  Quiero corregir montos y confirmar pagos
  Para que el balance refleje la realidad

  @listo @critico @requiere-semilla
  Escenario: Confirmar el monto de un gasto variable lo saca de revisión
    Dado que tengo un renglón con estado "NEEDS_REVIEW"
    Cuando ajusto su monto planeado
    Entonces la respuesta es 200
    Y el estado del renglón pasa a "PENDING"
    Y el monto planeado es el que envié

  @listo @requiere-semilla
  Escenario: Registrar un pago por el monto completo
    Dado que tengo un renglón pendiente
    Cuando confirmo que ocurrió por el monto planeado
    Entonces la respuesta es 200
    Y el estado del renglón es "PAID"
    Y el renglón guarda la fecha en que ocurrió

  @listo @critico @requiere-semilla
  Escenario: Registrar un pago menor al planeado queda parcial
    Dado que tengo un renglón pendiente
    Cuando confirmo que ocurrió por un monto menor al planeado
    Entonces el estado del renglón es "PARTIAL"

  @listo @requiere-semilla
  Escenario: El monto real entra en los totales reales
    Dado que tengo un ciclo con un gasto planeado
    Cuando confirmo ese gasto por un monto distinto al planeado
    Y consulto el balance del ciclo
    Entonces los totales planeados no cambian
    Y los totales reales reflejan el monto confirmado

  @listo @requiere-semilla
  Escenario: Quitar un renglón del ciclo no borra la plantilla
    Dado que tengo un renglón pendiente
    Cuando quito ese renglón del ciclo
    Entonces la respuesta es 200
    Y el estado del renglón es "SKIPPED"
    Y la plantilla que lo originó sigue activa

  @listo @requiere-semilla
  Escenario: Un renglón omitido se puede volver a dejar pendiente
    Dado que tengo un renglón omitido
    Cuando lo reabro
    Entonces el estado del renglón es "PENDING"

  @listo @requiere-semilla
  Escenario: Un renglón pagado se puede reabrir para corregir un error
    Dado que tengo un renglón pagado
    Cuando lo reabro
    Entonces el estado del renglón es "PENDING"
    Y el monto real vuelve a cero

  @listo @requiere-semilla
  Escenario: Reordenar un renglón cambia su posición en la lista
    Dado que tengo un ciclo con varios renglones
    Cuando cambio el orden de presentación de uno
    Y consulto los renglones del ciclo
    Entonces ese renglón aparece en la nueva posición

  @listo
  Escenario: Un monto negativo se rechaza
    Cuando ajusto el monto planeado de un renglón a un valor negativo
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

  @listo
  Escenario: Un monto que no es un número se rechaza
    Cuando ajusto el monto planeado de un renglón a un texto
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

  @listo
  Escenario: Un renglón que no existe da 404
    Cuando confirmo un renglón con un identificador inventado
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"
```

---

## Cierre e inmutabilidad

La regla más importante de todo el módulo: **un ciclo cerrado es historia, y la
historia no se reescribe.**

```gherkin
# language: es

@api @presupuesto @ciclos
Característica: Cerrar un ciclo
  Como persona que quiere confiar en su historial
  Quiero que un ciclo cerrado ya no cambie
  Para que las cifras de ayer sigan siendo las de ayer

  @listo @critico
  Escenario: Cerrar el ciclo en curso
    Dado que tengo un ciclo activo
    Cuando cierro el ciclo
    Entonces la respuesta es 200
    Y el estado del ciclo es "CLOSED"
    Y el ciclo guarda la fecha y hora de cierre

  @listo
  Escenario: Cerrar un ciclo ya cerrado se rechaza
    Dado que tengo un ciclo cerrado
    Cuando cierro el ciclo
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"

  @listo @critico @requiere-semilla
  Escenario: Un renglón de un ciclo cerrado no se puede ajustar
    Dado que tengo un ciclo cerrado con renglones
    Cuando ajusto el monto planeado de un renglón
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"
    Y el mensaje dice que el ciclo está cerrado

  @listo @critico @requiere-semilla
  Escenario: Un renglón de un ciclo cerrado no se puede confirmar
    Dado que tengo un ciclo cerrado con renglones
    Cuando confirmo un renglón
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"

  @listo @critico @requiere-semilla
  Escenario: Un renglón de un ciclo cerrado no se puede omitir ni reabrir
    Dado que tengo un ciclo cerrado con renglones
    Cuando quito un renglón del ciclo
    Entonces la respuesta es 422
    Cuando reabro un renglón
    Entonces la respuesta es 422

  @listo @critico @requiere-semilla
  Escenario: Editar una plantilla no altera los ciclos ya cerrados
    Dado que tengo un ciclo cerrado con un gasto de 6000
    Cuando cambio el monto de ese gasto en la plantilla
    Y consulto el balance del ciclo cerrado
    Entonces el renglón sigue valiendo 6000

  @listo @requiere-semilla
  Escenario: Cambiar el tipo de ciclo aplica desde el siguiente
    Dado que tengo un ciclo activo quincenal
    Cuando cambio mi tipo de ciclo a mensual
    Y consulto el ciclo en curso
    Entonces el periodo del ciclo actual sigue siendo quincenal
```

---

## Historial

```gherkin
# language: es

@api @presupuesto @ciclos
Característica: Historial de ciclos
  Como persona que quiere ver su evolución
  Quiero consultar mis ciclos anteriores
  Para comparar periodos

  @listo
  Escenario: El historial lista los ciclos del más reciente al más antiguo
    Dado que tengo varios ciclos
    Cuando consulto el historial de ciclos
    Entonces la respuesta es 200
    Y vienen ordenados por fecha de inicio descendente

  @listo
  Escenario: El historial se pagina
    Dado que tengo más ciclos que el tamaño de página
    Cuando consulto el historial de ciclos
    Entonces el cuerpo incluye el total de elementos y de páginas
    Y indica si es la última página

  @listo
  Escenario: Consultar un ciclo por su identificador
    Dado que tengo un ciclo
    Cuando consulto ese ciclo por su identificador
    Entonces la respuesta es 200
    Y el cuerpo describe el ciclo y su periodo

  @listo
  Escenario: Un ciclo que no existe da 404
    Cuando consulto un ciclo con un identificador inventado
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"
```

---

## Aislamiento entre usuarios

Ningún endpoint de este módulo recibe un identificador de usuario: siempre sale
del token. Estos escenarios verifican esa propiedad.

```gherkin
# language: es

@api @presupuesto @seguridad
Característica: Cada persona solo ve su propio presupuesto
  Como usuario de LUMA
  Quiero que mis cifras sean mías
  Para poder confiar en la aplicación

  @listo @critico @seguridad
  Escenario: El ciclo de otra persona no se puede consultar
    Dado que existe un ciclo de otro usuario
    Y estoy autenticado con mi cuenta
    Cuando consulto ese ciclo por su identificador
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"

  @listo @critico @seguridad
  Escenario: El renglón de otra persona no se puede modificar
    Dado que existe un renglón de un ciclo de otro usuario
    Y estoy autenticado con mi cuenta
    Cuando confirmo ese renglón desde mi propio ciclo
    Entonces la respuesta es 404

  @listo @seguridad
  Escenario: Un ciclo ajeno responde 404 y no 403
    Dado que existe un ciclo de otro usuario
    Cuando lo consulto
    Entonces la respuesta es 404
    Y la respuesta no revela que el ciclo exista

  @listo @seguridad
  Escenario: Ningún endpoint de ciclos acepta un identificador de usuario
    Cuando reviso la especificación OpenAPI del módulo de ciclos
    Entonces ninguna ruta incluye un identificador de usuario

  @listo @seguridad
  Escenario: Los errores del módulo no revelan detalles internos
    Cuando provoco un error en cualquier endpoint de ciclos
    Entonces la respuesta no contiene stack traces
    Y no contiene nombres de tabla ni SQL
```

> El 404 en lugar de 403 es intencional. Un 403 confirmaría que el recurso
> existe, y eso ya es información que no le corresponde a quien pregunta.

---

## Vencimientos

El trabajo en segundo plano que marca como vencido lo que pasó de fecha.

```gherkin
# language: es

@api @presupuesto @vencimientos
Característica: Renglones vencidos
  Como persona que puede olvidar un pago
  Quiero que la aplicación marque lo que ya se venció
  Para no enterarme tarde

  @listo @requiere-semilla
  Escenario: Un renglón pendiente cuya fecha ya pasó queda vencido
    Dado que tengo un renglón pendiente con fecha de ayer
    Cuando corre la revisión de vencimientos
    Y consulto los renglones del ciclo
    Entonces el estado del renglón es "OVERDUE"

  @listo @requiere-semilla
  Escenario: Un renglón pagado no se marca como vencido
    Dado que tengo un renglón pagado con fecha de ayer
    Cuando corre la revisión de vencimientos
    Entonces el estado del renglón sigue siendo "PAID"

  @listo @requiere-semilla
  Escenario: Un renglón omitido no se marca como vencido
    Dado que tengo un renglón omitido con fecha de ayer
    Cuando corre la revisión de vencimientos
    Entonces el estado del renglón sigue siendo "SKIPPED"

  @listo @requiere-semilla
  Escenario: Un renglón de un ciclo cerrado no se marca como vencido
    Dado que tengo un ciclo cerrado con un renglón pendiente y fecha pasada
    Cuando corre la revisión de vencimientos
    Entonces el estado del renglón no cambia

  @listo @requiere-semilla
  Escenario: Pagar un renglón vencido lo deja pagado
    Dado que tengo un renglón vencido
    Cuando confirmo que ocurrió
    Entonces el estado del renglón es "PAID"
```

---

## Lo que falta

| Qué | Cuándo |
|---|---|
| Escenarios `@ui` de la pantalla del ciclo | Fase 10 |
| Sustituir `@requiere-semilla` por llamadas al CRUD | Fases 5 a 8 |
| Política de prorrateo | Sin fecha — hoy falla de forma explícita |
| Reordenar renglones arrastrando | Fase 10 |
