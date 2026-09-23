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
