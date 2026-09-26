# Convenciones de la API

El catalogo de endpoints **no se documenta aqui**. Se genera desde el codigo y
vive en Swagger UI: http://localhost:8080/swagger-ui.html

Este documento cubre lo que Swagger no explica: las reglas transversales.

---

## Base y versionado

```
/api/v1/...
```

La version va en la ruta. Cuando exista `/api/v2`, `/api/v1` sigue funcionando
hasta que ningun cliente la use — una aplicacion movil instalada no se actualiza
sola.

## Autenticacion

```
Authorization: Bearer <accessToken>
```

La sesion son dos piezas con vidas muy distintas:

| Pieza | Vive en | Viaja en | Dura |
|---|---|---|---|
| Access token (JWT) | Memoria del cliente | Cabecera `Authorization` | 15 minutos |
| Token de renovacion | Cookie HttpOnly | Cookie `luma_rt`, ruta `/api/v1/auth` | 30 dias |

El access token autoriza cada peticion. El de renovacion solo sirve para
conseguir uno nuevo, y el JavaScript de la pagina no puede leerlo.

**Cada token de renovacion sirve una sola vez.** `POST /auth/refresh` emite uno
nuevo y revoca el anterior. Si aparece un token ya rotado despues del margen de
carrera de 30 segundos, se asume copia robada y se revoca la sesion completa del
usuario.

### Clientes moviles

Una aplicacion nativa no tiene donde recibir una cookie. Con la cabecera
`X-Client-Type: mobile`, el token de renovacion llega en el cuerpo de la
respuesta (campo `refreshToken`) y se devuelve en la cabecera `X-Refresh-Token`.

### Endpoints publicos

| Metodo | Ruta | Como se autentica |
|---|---|---|
| POST | `/api/v1/auth/register` | — |
| POST | `/api/v1/auth/login` | — |
| POST | `/api/v1/auth/refresh` | Cookie de renovacion |
| POST | `/api/v1/auth/logout` | Cookie de renovacion |
| POST | `/api/v1/auth/password/forgot` | — |
| POST | `/api/v1/auth/password/reset` | Codigo del enlace |
| GET | `/api/v1/system/**` | — |
| GET | `/actuator/health` | — |

`refresh` y `logout` se autentican con la cookie y no con el access token: se
usan justamente cuando el access token ya no sirve.

Todo lo demas requiere `Authorization: Bearer`.

## Propiedad de los datos

**Ningun endpoint recibe un `userId`.** El usuario siempre se resuelve desde el
token.

```
GET /api/v1/incomes            correcto
GET /api/v1/users/42/incomes   nunca
```

Esto elimina por construccion la posibilidad de leer los datos de otro usuario
cambiando un numero en la URL.

## Importes

Los importes viajan como **cadena**, no como numero:

```json
{ "amount": "12500.00", "currency": "MXN" }
```

El tipo numerico de JavaScript pierde precision con importes grandes. En una
aplicacion de dinero eso no es aceptable, asi que la conversion la hace el
cliente al mostrar, no el transporte.

## Fechas

- Instantes: ISO-8601 en UTC — `2026-09-13T10:00:00Z`
- Fechas de calendario (vencimientos, inicio de ciclo): `2026-09-13`, sin hora
  ni zona horaria. El dia 1 es el dia 1 en cualquier parte.

## Paginacion

```
GET /api/v1/incomes?page=0&size=20&sort=AMOUNT_DESC
```

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

Se expone esta forma y no el `Page` de Spring Data para que el contrato sobreviva
a cambios internos de la libreria.

**`sort` solo existe donde el orden es una decision del cliente.** Donde el orden
lo fija el producto — el historial de ciclos va del mas reciente al mas antiguo,
los renglones por fecha de vencimiento — el endpoint recibe unicamente `page` y
`size`. Un parametro de orden libre acepta cualquier nombre de campo, y uno que no
existe revienta la consulta: el cliente podria tumbar el endpoint con un dato
invalido.

Donde si se ofrece, `sort` es una **lista cerrada de opciones con nombre**, no un
campo de la entidad: `NEWEST`, `AMOUNT_DESC`, `NAME`. Un valor desconocido lo
rechaza la conversion de tipos de Spring con **400 / `VALIDATION_ERROR`**, sin
codigo de validacion propio, y ningun nombre de columna sale al contrato publico.
Renombrar un campo de la entidad no rompe a ningun cliente.

`size` tiene tope. Sin tope, una peticion puede pedir la tabla completa.

## Errores

Base RFC 9457 (`ProblemDetail`) mas las extensiones del proyecto.

```json
{
  "type": "https://luma.app/errors/validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "Revisa los campos marcados.",
  "instance": "/api/v1/incomes",
  "timestamp": "2026-09-13T10:00:00Z",
  "errorCode": "VALIDATION_ERROR",
  "traceId": "8f3c1a2b-4d5e-6f70-8192-a3b4c5d6e7f8",
  "errors": [
    { "field": "amount", "message": "El monto debe ser mayor que cero" }
  ]
}
```

**El cliente decide que mostrar segun `errorCode`, no segun el texto.** El texto
puede traducirse o reescribirse; el codigo es estable.

| `errorCode` | HTTP | Cuando |
|---|---|---|
| `VALIDATION_ERROR` | 400 | La peticion esta mal formada |
| `UNAUTHORIZED` | 401 | Falta el token o expiro |
| `FORBIDDEN` | 403 | Autenticado pero sin acceso |
| `RESOURCE_NOT_FOUND` | 404 | No existe, o no es del usuario |
| `CONFLICT` | 409 | Choca con el estado actual |
| `BUSINESS_RULE_VIOLATION` | 422 | Una regla de negocio lo impide |
| `INTERNAL_ERROR` | 500 | Fallo inesperado |

Lo que **nunca** aparece en una respuesta de error: stack traces, mensajes de
excepcion internos, nombres de tabla, SQL, ni cualquier dato sensible.

## Identificador de correlacion

Cada peticion lleva o recibe un `X-Correlation-Id`. El mismo valor aparece en los
logs del backend y en el campo `traceId` del error. Un usuario puede reportar un
problema con esa referencia y lleva directo a la linea de log.

```
X-Correlation-Id: 8f3c1a2b-4d5e-6f70-8192-a3b4c5d6e7f8
```

## Identificadores

Los recursos se exponen con UUID, nunca con el id secuencial interno. Un id
autoincremental permite enumerar recursos y revela el volumen del negocio.

---

## Ciclos presupuestales

El catálogo de rutas está en Swagger, bajo la etiqueta **Budget cycles**. Lo que
Swagger no puede explicar son las reglas que gobiernan ese módulo.

### Un ciclo cerrado es inmutable

Ningún renglón de un ciclo `CLOSED` se puede ajustar, confirmar, omitir ni
reabrir. Cualquier intento responde **422 / `BUSINESS_RULE_VIOLATION`**.

No es una restricción de interfaz: se aplica en la capa de aplicación, en el
único punto donde puede aplicarse de verdad — la escritura.

La razón es de producto: si el historial cambia, no sirve para nada. Un ciclo
cerrado es una fotografía de cómo estuvo ese periodo.

### Los renglones son copias, no referencias

Al abrir un ciclo, cada ingreso, gasto y meta activa se materializa como uno o
más renglones con nombre, monto, categoría y fecha **copiados** en ese momento.

Consecuencia que conviene tener clara al consumir la API: editar un gasto **no
altera ningún ciclo ya abierto**. El cambio se ve en el siguiente.

Una plantilla puede producir varios renglones en un mismo ciclo — una renta
mensual cae dos veces en un bimestre, un sueldo quincenal dos veces en un mes.
Cada ocurrencia es su propio renglón con su propia fecha.

### Plan y ejecución viajan juntos

Todo balance trae dos bloques de totales:

| Bloque | Qué es |
|---|---|
| `planned` | Lo que el ciclo dijo que pasaría |
| `actual` | Lo que la persona ha confirmado que pasó |

El estado del presupuesto (`SURPLUS` / `BALANCED` / `DEFICIT`) se deriva del
balance **planeado**: la pregunta que responde es «¿me va a alcanzar?», no
«¿me alcanzó?».

Los renglones `SKIPPED` no entran en ninguno de los dos.

### Estados de un renglón

```
                 ┌──────────────┐
                 │ NEEDS_REVIEW │  gasto variable recién materializado
                 └──────┬───────┘
                        │  confirmar monto
                        v
  ┌─────────┐      ┌─────────┐      ┌──────────┐
  │ SKIPPED │<-----│ PENDING │----->│ OVERDUE  │  pasó la fecha
  └────┬────┘      └────┬────┘      └────┬─────┘
       │ reabrir        │ confirmar      │ confirmar
       └───────────────>│<───────────────┘
                        v
                 ┌──────────────┐
                 │ PAID/PARTIAL │  según si cubrió el monto planeado
                 └──────────────┘
```

**`SKIPPED` no es lo mismo que no haber pagado.** Quitar un renglón del ciclo lo
saca del presupuesto sin borrar nada ni desactivar la plantilla: sirve para «este
mes no aplica». Lo que no se pagó y ya venció es `OVERDUE`.

Reabrir un renglón pagado devuelve el monto real a cero. Es la salida para
corregir un error de captura.

### Abrir el siguiente ciclo

Un solo endpoint hace dos cosas, y el orden importa:

1. Si hay un ciclo activo **cuyo periodo ya terminó**, se cierra solo. Exigir un
   cierre manual no aportaría nada.
2. Si el periodo del ciclo activo **no ha terminado**, la petición se rechaza con
   **422**: abrir el siguiente sería saltarse el actual.

Los ciclos son contiguos por construcción — el siguiente empieza el día después
de que acaba el anterior — así que la secuencia no puede desincronizarse.

### Filtros de renglones

`type` y `status` son mutuamente excluyentes. Si llegan los dos, gana `type`.
Un valor que no existe en el enum responde **400 / `VALIDATION_ERROR`**.

### Lo que todavía no está

La preferencia `expenseAllocationPolicy` acepta `PRORATE`, pero materializar con
esa política responde **422** con un mensaje que pide cambiarla a `BY_DUE_DATE`.

Es deliberado. Caer en silencio a la otra política daría un presupuesto
calculado con una regla distinta de la que la persona eligió, y un número
equivocado en una aplicación de dinero es peor que un error claro.

---

## Ingresos

### Un ingreso es una plantilla; el ciclo guarda copias

De ahi una asimetria que conviene tener clara al consumir la API:

| Accion | Ciclo en curso | Ciclos siguientes |
|---|---|---|
| Capturar uno nuevo | **Entra** si cae en el periodo | Entra |
| Editar | **No cambia** | Aplica |
| Desactivar | **No cambia** | No entra |
| Eliminar | **No cambia** | No entra |

Capturar el sueldo a media quincena y ver un presupuesto sin ingresos no se le
puede explicar a nadie, asi que lo nuevo si entra. Pero si editar reescribiera
los renglones, lo que la persona reviso ayer podria ser otra cosa hoy.

### Los ingresos de monto variable piden confirmacion

Los tipos `VARIABLE` y `SALE` se materializan en `NEEDS_REVIEW`, igual que un
gasto variable. Un presupuesto que da por seguras unas comisiones que todavia no
se conocen promete dinero que puede no llegar. El campo `requiresReview` de la
respuesta expone la regla ya calculada, para que la interfaz pueda advertirlo al
capturar sin reimplementarla.

### Desactivar no es eliminar

`deactivate` deja de contarlo y se revierte con `activate`. `DELETE` es **borrado
logico**: la fila se conserva porque los renglones de ciclos pasados guardan su
origen, pero el ingreso desaparece de las listas y ya no se reactiva.

### Editar manda solo lo que cambia

`PATCH` ignora los campos nulos. La excepcion es el calendario: `frequency`,
`expectedDay`, `startDate` y `endDate` se aplican JUNTOS, porque son una sola
decision y mezclar una frecuencia nueva con un dia viejo produce combinaciones
que nadie pidio. Si mandas cualquiera de los cuatro, manda tambien la frecuencia
y la fecha de inicio.

### El orden de la lista es una lista cerrada

`?sort=` acepta `NEWEST` (por omision), `NAME`, `AMOUNT_DESC`, `AMOUNT_ASC` o
`START_DATE`. No es un nombre de campo libre: un valor desconocido responde
**400 / `VALIDATION_ERROR`**, y ningun nombre de columna sale al contrato
publico.

---

## Gastos

### Fijos y variables son un solo recurso

`/api/v1/expenses` los cubre a ambos. Lo unico que los distingue es `kind`
(`FIXED` o `VARIABLE`), y separarlos en dos rutas duplicaria el contrato entero
por un campo. Ver `docs/00-arquitectura-fase-0.md` seccion 1.3.

La diferencia esta en como se materializan:

| | `FIXED` | `VARIABLE` |
|---|---|---|
| Estado del renglon al entrar al ciclo | `PENDING` | `NEEDS_REVIEW` |
| El balance lo da por seguro | Si | No, hasta que la persona confirme |

### La flexibilidad no es decorativa

`flexibility` es obligatoria: `CRITICAL`, `IMPORTANT` o `FLEXIBLE`. Es lo que
permitira al modulo de analisis (Fase 13) distinguir la renta de una suscripcion
de musica, y **nunca sugerir retrasar un pago critico**.

El renglon del ciclo guarda su propia copia. Es deliberado: el analisis mira el
ciclo, y la plantilla pudo cambiar despues de cerrarlo.

### La categoria es opcional y se referencia por su UUID

`categoryId` acepta el identificador publico de una categoria del catalogo, o
nulo. Exigirla siempre solo lograria que la gente eligiera "Otros" para salir
del paso, que es peor dato que ninguno.

Una categoria que no existe, o que pertenece a otra persona, responde **404**:
un gasto con una categoria invalida ensucia el analisis sin que nadie se entere.

El catalogo se consulta en `GET /api/v1/expense-categories` y trae las del
sistema mas las propias. No se pagina: son pocas y el cliente las necesita todas
para pintar un desplegable. Hoy es de solo lectura.

### Quitar la categoria necesita una bandera

En `PATCH`, "no mande el campo" y "quiero quitarsela" llegan los dos como nulo y
significan lo contrario. Por eso existe `clearCategory`:

```json
{ "clearCategory": true }
```

Sin esa bandera, un `categoryId` nulo se ignora y la categoria se queda como
estaba.

### Orden de la lista

`?sort=` acepta `NEWEST` (por omision), `NAME`, `AMOUNT_DESC`, `AMOUNT_ASC` o
`DUE_DAY`. Misma regla que en ingresos: lista cerrada, y un valor desconocido
responde 400.

---

## Resumen financiero

### Ninguna cifra se deriva en el cliente, y eso moldea el contrato

`outflowChange` en `/budget-cycles/trends` llega con la **dirección separada de
la magnitud**:

```json
{ "direction": "UP", "amount": { "amount": "800.00", "currency": "MXN" } }
```

`direction` es `UP`, `DOWN` o `SAME`; `amount` viaja **siempre en positivo**. Si
llegara un solo número con signo, el cliente tendría que restar y sacarle el
valor absoluto para escribir «gastaste $800 más» — aritmética de dinero en el
lugar equivocado.

Por la misma razón, con déficit el balance llega **negativo**. La interfaz lo
muestra con su signo; no existe un campo con el valor absoluto porque no hace
falta inventarlo.

### El consejo de déficit propone, no ejecuta

`GET /budget-cycles/{cycleId}/advice` no modifica nada. Devuelve los renglones de
los que se puede recortar, en el orden en que conviene mirarlos:

1. **Gastos flexibles.** «Flexible» significa que se puede mover.
2. **Ahorros, del menos prioritario al más.** No apartar este ciclo no le cuesta
   nada a nadie hoy, y el siguiente lo retoma. La prioridad de las metas sirve
   exactamente para esto.
3. **Gastos importantes.** Al final, porque retrasarlos sí tiene consecuencia.

Dentro de cada grupo, primero el monto más grande: así se llega a la cifra con la
menor cantidad de renuncias.

**Los gastos `CRITICAL` no aparecen nunca.** No es una heurística ajustable: es
lo que la aplicación promete al capturarlos, y hay una prueba que falla si
alguien lo cambia.

Tampoco se proponen los renglones ya confirmados —el dinero ya salió— ni los
ingresos, que recortarlos empeoraría el déficit.

`coversTheGap: false` significa que **aun moviendo todo lo propuesto no
alcanza**. La interfaz tiene que decirlo: una lista presentada como solución sin
serlo es peor que no dar ninguna.

Sin déficit responde `missing: 0` y la lista vacía. No es 404: preguntar «qué
hago» con el ciclo en orden tiene respuesta, y es «nada».

### La comparación se lee como línea de tiempo

`GET /budget-cycles/trends?cycles=6` responde del **más antiguo al más
reciente**, al revés que el historial. Un historial es una lista —lo último
primero—; una comparación es una línea de tiempo, y si llegara invertida la
gráfica saldría al revés. Invertirla en el cliente sería pedirle que sepa para
qué va a usar el dato.

Tope de 12 ciclos: un año de mensuales o medio de quincenales. Más no cabe
legible en una gráfica.

El primero de la lista no trae `outflowChange`: no tiene con qué compararse.

---

## Alta guiada

### El asistente no tiene API propia para capturar

Los ingresos, los gastos y las metas se crean con **sus propios endpoints**, los
mismos que usa el resto de la aplicación. `/onboarding` solo tiene tres
operaciones y ninguna recibe datos del presupuesto:

| Endpoint | Qué hace |
|---|---|
| `GET /onboarding/state` | Cuánto lleva la cuenta y si ya puede terminar |
| `POST /onboarding/complete` | Marca el alta y abre el primer ciclo |
| `POST /onboarding/skip` | Marca el alta sin abrir ciclo |

No hay borrador. Capturar un ingreso en el asistente **crea el ingreso**. Así no
existen dos formas de crear la misma cosa que puedan validar distinto, y volver
después no necesita recordar nada: el servidor ya sabe qué hay.

### `resumeStep` se deduce, no se guarda

Sin ingresos devuelve `INCOMES`; con al menos uno, `SUMMARY`. Es una **sugerencia
para quien vuelve**, no una reja: desde el asistente se puede ir a cualquier
paso.

Un número de paso guardado mentiría en cuanto la persona borrara algo desde otra
pantalla, y costaría una columna para un dato que se puede calcular.

### Terminar exige un ingreso y abre el ciclo en la misma transacción

`POST /onboarding/complete` responde **422** sin ingresos activos: sin ellos el
resumen diría «te quedan $0» y eso no es información.

La marca y la apertura del ciclo van juntas a propósito. Una cuenta marcada como
configurada pero sin ciclo llegaría al resumen sin nada que mostrar y sin forma
de volver al asistente.

Repetirlo responde 422: un segundo `complete` abriría un ciclo de más.

`POST /onboarding/skip` es lo contrario en todo — no exige nada, no abre ciclo y
es idempotente. Lo capturado se conserva: es de la persona.

### Lo capturado en un alta abandonada se invalida

Un trabajo diario recorre las cuentas con `onboarding_completed_at` nulo y borra
lógicamente sus ingresos, gastos y metas cuando la última captura es más vieja
que `LUMA_ONBOARDING_ABANDON_AFTER` (una semana por omisión; `0` lo apaga).

Tres cosas que conviene saber si se consume esta API:

- **Invalida, no borra.** Las filas siguen existiendo con `deleted_at`, así que
  dejan de salir en cualquier consulta del producto pero se pueden auditar.
- **Se mide desde la última captura**, no desde el registro. Quien se registró
  hace un mes y empezó hoy no se toca.
- **Una cuenta que pospuso el alta nunca se limpia**, porque `skip` ya la marcó
  como terminada.

No hizo falta una columna para distinguir «esto vino del alta»: el asistente es
obligatorio, así que una cuenta sin esa fecha no tiene otra forma de haber
creado nada. La ausencia de la fecha es la marca.

### Preferencias del ciclo

`GET` y `PATCH /users/me/preferences` — la ruta dice `me` y no un identificador
porque no existe forma de nombrar a otra persona.

`PATCH /users/me/preferences/cycle` aplica al **siguiente** ciclo. El que esté
abierto conserva su periodo: reescribirlo cambiaría las fechas de un presupuesto
que ya se está usando.

Un día de anclaje fuera de 1–31 responde 422.

---

## Revision del ciclo

### Confirmar el monto y registrar el pago son dos cosas

| | `POST .../items/confirm-amounts` | `POST .../items/{id}/settle` |
|---|---|---|
| Qué afirma | cuánto es este ciclo | que ya ocurrió |
| Campo que cambia | `plannedAmount` | `actualAmount`, `settledOn` |
| Estado resultante | `PENDING` | `PAID` o `PARTIAL` |
| En lote | sí | no |

Un gasto variable nace en `NEEDS_REVIEW` porque no se sabe cuánto es. Saberlo no
es haberlo pagado, y un endpoint que hiciera las dos cosas dejaría el balance
dando por pagado lo que nadie pagó.

### La revisión es solo del ciclo en curso

`GET /budget-cycles/current/review`. No hay versión por `{cycleId}` a propósito:
un ciclo cerrado es inmutable, así que no hay nada que revisar en él.

Sin ciclo abierto responde **404**, igual que `/current`. No es lo mismo que una
lista vacía —«no tienes ciclo» y «no te falta nada» son estados distintos— y
colapsarlos obligaría al cliente a adivinar cuál de los dos mostrar.

### La sugerencia es el ciclo anterior, y solo lo confirmado

`suggestedAmount` es lo que se **confirmó** del mismo gasto en el ciclo
inmediatamente anterior, con `suggestedFromStart` diciendo de cuándo es.

Es nulo cuando no hay ciclo anterior, cuando ese ciclo no tuvo ese gasto, o
cuando lo tuvo y nadie lo confirmó. Un monto planeado sin confirmar es un plan:
sugerirlo propagaría la misma estimación de ciclo en ciclo hasta hacerla parecer
un dato.

No es un promedio. Un promedio de seis ciclos diluye justo el salto que importa
cuando un recibo acaba de subir; el promedio está en el historial, junto a las
cifras de las que sale.

### El historial trae su promedio calculado

`GET /budget-cycles/{cycleId}/items/{itemId}/history` responde `cycles`,
`average` y `entries` — hasta seis ciclos, del más reciente al más antiguo, y
solo los confirmados.

`average` viene del servidor aunque sea una cifra de lectura: es dinero, y
ninguna cifra de dinero se deriva en el cliente. Dos formas de redondear el
mismo número es como empiezan las cuentas que no cuadran. Es nulo cuando no hay
historia.

Cada entrada trae el planeado **y** el real: la diferencia entre ambos es lo que
revela que un gasto lleva ciclos costando más de lo presupuestado.

### El lote es todo o nada

`POST /budget-cycles/{cycleId}/items/confirm-amounts` aplica los montos en una
sola transacción. Si un renglón falla, no se confirma ninguno: una confirmación
a medias dejaría a la persona sin saber cuáles quedaron.

Un `itemId` repetido en el lote responde **422** en vez de quedarse con el
último en silencio — dos montos para el mismo renglón significa que el cliente
armó mal la petición, y elegir uno por él se vería como un monto perdido.

Tope de 200 renglones. No es una defensa del servidor: un lote más grande no
sale de una pantalla de revisión.

---

## Ahorros

### El modo de aporte decide qué campos son obligatorios

`mode` no es una preferencia: determina si la meta resta del presupuesto y quién
calcula cuánto.

| `mode` | Obligatorio | ¿Resta del presupuesto? |
|---|---|---|
| `AUTO_BY_TARGET_DATE` | `targetDate` | Sí |
| `FIXED_PER_CYCLE` | `plannedPerCycle` | Sí |
| `MANUAL` | — | No |

Faltar el campo del modo elegido responde **422**, no 400: no es un dato mal
escrito sino una combinación que no se sostiene.

### `plannedPerCycle` solo está guardado cuando la persona lo fijó

Con `AUTO_BY_TARGET_DATE` el aporte se recalcula en cada ciclo —depende de
cuánto falta hoy y de cuántos ciclos quedan—, así que el recurso responde
`0.00`. El monto real de este ciclo está en el renglón `SAVING` del ciclo, no
aquí.

El cliente no debe derivarlo: `GET /budget-cycles/{id}/items` ya trae la cifra
que el motor calculó.

### El progreso viaja como número; los importes, como cadena

`progress` va de 0 a 1 con cuatro decimales y es un **número** JSON. Es la única
excepción a la regla de [Importes](#importes), y no la contradice: una
proporción de cuatro decimales no pierde precisión en el tipo numérico de
JavaScript, un importe grande sí.

Viene **recortado a 1**. Pasarse de la meta no produce 1.3: la barra se llena y
`remaining` es `0.00`.

### El signo lo decide el tipo, no quien llama

En `POST /savings-goals/{id}/movements`, `amount` va siempre **positivo** y
`type` decide qué significa:

| `type` | Efecto |
|---|---|
| `EXTRA` | Suma al progreso |
| `WITHDRAWAL` | Resta del progreso; se guarda en negativo |

`PLANNED` no se acepta aquí: esos movimientos nacen de confirmar el renglón del
ciclo. Aceptar montos negativos permitiría registrar un retiro disfrazado de
aportación.

Un retiro que dejaría el progreso en negativo responde 422.

### Confirmar el renglón de ahorro no puede contar dos veces

`POST /budget-cycles/{cycleId}/items/{itemId}/settle` sobre un renglón `SAVING`
registra el aporte en la meta. El movimiento guarda de qué renglón salió, y un
segundo `settle` sobre el mismo renglón **no vuelve a sumar**: no falla, no hace
nada. Confirmar es idempotente a propósito — quien confirma quiere que quede
confirmado, no enterarse de que ya lo estaba.

`registerInGoal: false` confirma el renglón sin tocar la meta. Es para cuando el
dinero salió del presupuesto pero no llegó al ahorro.

Si la meta se eliminó después de abrir el ciclo, el `settle` responde 200 igual:
el renglón sigue siendo válido para el presupuesto, solo que no hay meta a la
cual sumarle.

### El orden se manda completo

`PUT /savings-goals/order` recibe la lista **completa** de identificadores, de
mayor a menor prioridad, y no «mueve esta al lugar N». Así el resultado no
depende de en qué estado creía el cliente que estaban las metas: con dos
pestañas abiertas, la última en guardar gana de forma predecible en vez de
dejarlas intercaladas.

Una lista incompleta, o con un identificador repetido, responde 422.

### Quitar la fecha objetivo necesita una bandera

Misma regla que la categoría de un gasto: en `PATCH`, «no mandé el campo» y
«quítasela» llegan los dos como nulo.

```json
{ "clearTargetDate": true }
```

Lo mismo aplica al resto de la edición parcial: cambiar de modo **sin** mandar
`plannedPerCycle` conserva el que ya había, y mandar `icon` sin `color` no borra
el color.

### No se pagina

Una persona tiene metas, no cientos, y reordenarlas por prioridad exige tenerlas
todas a la vista. `GET /savings-goals` responde un arreglo, no una página.
Acepta `?status=` para filtrar por `ACTIVE`, `COMPLETED`, `PAUSED` o `CANCELED`.

### Eliminar es borrado lógico

`DELETE` responde 204 y la meta desaparece de las listas, pero se conserva: los
ciclos anteriores que tienen su renglón siguen teniendo explicación.

## Alertas

### Tres condiciones vigiladas por LUMA, no por quien usa la app

`GET /notifications` devuelve alertas de tres tipos:

| `type` | Cuándo nace |
|---|---|
| `PAYMENT_DUE_SOON` | Un renglón vence en 3 días |
| `PAYMENT_OVERDUE` | Un renglón pasó su fecha sin marcarse pagado |
| `CYCLE_DEFICIT` | El ciclo activo no alcanza con lo planeado |

`itemName`, `amount` y `dueDate` llegan nulos cuando el tipo no los usa: un
déficit no señala un renglón, así que viajan vacíos. El cliente arma la oración
según `type` con un mapa fijo en la interfaz, igual que ya hace con `BudgetState`
en el resumen financiero: nunca calculando el texto.

### Los tres días no son una preferencia

`PAYMENT_DUE_SOON` avisa siempre 3 días antes del vencimiento. Es una constante
del job, no un ajuste de usuario: la decisión más simple que cumple el
escenario, sin abrir una preferencia nueva hasta que alguien la pida de verdad.

### Una alerta por condición hasta que se resuelva

El job que revisa pagos próximos y déficits corre una vez al día y puede volver
a ver la misma condición muchas veces mientras dure. No genera una fila nueva
cada vez: `(userId, type, referenceId)` es único a nivel de base de datos, y el
servicio comprueba lo mismo antes de insertar (el doble candado, para cuando el
job corriera dos veces a la vez).

La alerta desaparece de "no leídas" solo cuando la persona la marca; la
condición en sí se resuelve sola cuando cambia la referencia: un ciclo cerrado
da paso a uno con otro id, un renglón pagado ya no vuelve a vencerse.

`PAYMENT_OVERDUE` no nace de este job: nace del mismo evento que
`OverdueItemsJob` dispara al marcar un renglón `OVERDUE` la primera vez, así
que tampoco se repite día a día.

### Solo viven adentro de la aplicación

No hay correo ni push para estas alertas: es la decisión que documenta
`architecture.md` ("Alertas internas"). Se consultan con `GET /notifications`,
se cuentan las no leídas con `GET /notifications/unread-count` (para la
insignia de la campana), y se marcan con `POST /notifications/{id}/read` o,
todas a la vez, con `POST /notifications/read-all`.

### Leídas y no leídas viajan juntas

`GET /notifications` no filtra por estado: la lista trae todo, de la más
reciente a la más vieja, y el menú desplegable distingue estilo por el campo
`read`, no con una segunda petición.


## Análisis financiero

### Un solo endpoint, tres señales independientes

`GET /insights` devuelve las tres señales del análisis sin IA, todas en la
misma respuesta:

```json
{
  "deficitCause": { "cycleId": "...", "missing": {...}, "categoryName": "Despensa", "previousAmount": {...}, "currentAmount": {...}, "increase": {...} },
  "surplusAllocation": { "cycleId": "...", "surplus": {...}, "shares": [{ "goalId": "...", "goalName": "Vacaciones", "amount": {...} }] },
  "categoryGrowth": [{ "categoryName": "Salud", "firstAmount": {...}, "lastAmount": {...}, "cycles": 3 }]
}
```

Un solo endpoint, no tres, porque las tres preguntas comparten la misma
lectura del ciclo actual y del historial de categorías: separarlas en tres
peticiones multiplicaría las consultas sin que el cliente necesite pedirlas
por separado.

### Ninguna cifra se inventa

`deficitCause` y `surplusAllocation` llegan `null` con mucha frecuencia, y no
es un error: `deficitCause` solo existe si el ciclo activo está en déficit **y**
hay un ciclo anterior con qué compararlo; `surplusAllocation` solo existe si el
ciclo activo tiene remanente. Sin ciclo abierto, ambos son `null` y
`categoryGrowth` viene vacío. `categoryGrowth` solo lista una categoría cuando
subió en **cada uno** de los últimos 3 ciclos y el último monto es positivo —
con menos de 3 ciclos de historia la lista viene vacía, nunca con una
suposición a medias.

`deficitCause` compara categorías, no ingreso contra gasto: es la causa más
angosta que las cifras respaldan sin adivinar, no "la" causa en un sentido
estricto. `increase` (`currentAmount - previousAmount`) viaja calculado porque
es la razón exacta por la que se eligió esa categoría entre todas.

### El reparto del remanente es una sugerencia, no un movimiento

`surplusAllocation.shares` llega en cascada por prioridad — llena primero la
meta de mayor prioridad, y sigue con la siguiente hasta agotar el remanente o
las metas —, en el mismo orden en que ya se usa la prioridad en el consejo de
déficit (`DeficitAdvisor`), pero en dirección contraria. `shares` viene vacío
cuando no hay ninguna meta activa: el remanente sigue siendo real y se
muestra, solo no hay a dónde proponer que vaya. Nada se registra en la meta
por consultar este endpoint; la persona aporta desde Ahorros si decide
seguir la sugerencia.

### Umbrales fijos, no preferencias

Tres ciclos seguidos al alza, y dos ciclos (el actual y el anterior) para
explicar un déficit: son constantes del servicio, no algo que la persona
configure. La misma decisión que ya se tomó con los 3 días de
`PAYMENT_DUE_SOON`.

### La capa de IA queda pendiente

`architecture.md` (§8.4) ya recomendaba un motor de reglas primero y un LLM
después —y quizá nunca para la mitad—. Este endpoint es esa primera capa,
completa. La redacción y priorización con un modelo de lenguaje, cuando
exista, solo interpretará estas mismas cifras: nunca las calculará.
