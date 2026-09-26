# Escenarios de prueba

Especificación del comportamiento de LUMA en Gherkin, alineada por funcionalidad.

**Aquí no hay automatización.** No hay step definitions, ni page objects, ni
configuración de runner, ni reportes. Estos documentos describen *qué* debe
hacer la aplicación; el *cómo* automatizarlo se construye por separado.

---

## Cómo usar esto

Cada bloque ` ```gherkin ` es un `.feature` válido tal cual. Al armar el
proyecto de automatización, se copian sin traducir.

Los archivos llevan la cabecera `# language: es`, así que cualquier herramienta
compatible con Cucumber (incluido playwright-bdd) los parsea con palabras clave
en español. Para pasarlos a inglés basta cambiar esa línea y las palabras clave.

---

## Estado por funcionalidad

Solo se automatiza lo que existe. Un escenario `@pendiente` describe una
pantalla que todavía no está construida.

| Documento | Funcionalidad | Estado |
|---|---|---|
| [01-autenticacion.md](01-autenticacion.md) | Registro, inicio de sesión, cierre de sesión, rutas protegidas | ✅ Automatizable |
| [02-navegacion-y-tema.md](02-navegacion-y-tema.md) | Navegación SPA, responsive, tema claro/oscuro, 404 | ✅ Automatizable |
| [03-dashboard.md](03-dashboard.md) | Estado de la conexión, estados de carga y error | ✅ Automatizable |
| [04-api-autenticacion.md](04-api-autenticacion.md) | Contrato HTTP: códigos, formato de error, seguridad | ✅ Automatizable |
| [05-recuperacion-contrasena.md](05-recuperacion-contrasena.md) | Recuperación y cambio de contraseña, con verificación por correo | ✅ Automatizable |
| [06-ciclos-presupuestales.md](06-ciclos-presupuestales.md) | Ciclos, materialización, balance, renglones, cierre e inmutabilidad | ⚠️ Automatizable por API; parte necesita semilla |
| [07-ingresos.md](07-ingresos.md) | Captura de ingresos, pantalla y relación con el ciclo abierto | ✅ Automatizable |
| [08-gastos.md](08-gastos.md) | Gastos fijos y variables, categorias y relacion con el ciclo | ✅ Automatizable |
| [09-ahorros.md](09-ahorros.md) | Metas de ahorro, movimientos, prioridad y confirmacion del aporte del ciclo | ✅ Automatizable |
| [10-revision-del-ciclo.md](10-revision-del-ciclo.md) | Revision de gastos variables, sugerencia entre ciclos, historial y confirmacion en lote | ✅ Automatizable |
| [11-alta-guiada.md](11-alta-guiada.md) | Asistente de configuracion inicial y limpieza de altas abandonadas | ✅ Automatizable |
| [12-resumen-financiero.md](12-resumen-financiero.md) | Balance del ciclo, consejo de deficit, comparacion entre ciclos y cierre | ✅ Automatizable |
| [13-alertas.md](13-alertas.md) | Alertas de pago proximo, pago vencido y deficit; campana con contador de no leidas | ✅ Automatizable |
| [14-anuncios.md](14-anuncios.md) | Espacio de anuncio del shell; monetizacion por publicidad, sin planes de pago | ✅ Automatizable |
| [99-escenarios-por-fase.md](99-escenarios-por-fase.md) | Todo lo que llega en fases posteriores | ⏳ Pendiente |

Todas las funcionalidades del MVP tienen interfaz. Los escenarios `@ui` del
módulo de ciclos, que estuvieron pendientes desde la Fase 4, viven ahora en
[12-resumen-financiero.md](12-resumen-financiero.md).

---

## Convención de etiquetas

| Etiqueta | Significado |
|---|---|
| `@ui` | Se ejercita por el navegador |
| `@api` | Se ejercita por peticiones HTTP directas |
| `@listo` | La funcionalidad existe: automatizable hoy |
| `@pendiente` | La funcionalidad aún no existe |
| `@requiere-semilla` | Existe, pero el `Dado` no se puede armar por la API todavía |
| `@smoke` | Entra en la suite mínima de humo |
| `@critico` | Su fallo bloquea el uso del producto |
| `@seguridad` | Verifica una propiedad de seguridad |
| `@responsive` | Requiere un viewport concreto |
| `@fase-N` | Fase en la que se construye la funcionalidad |

Un escenario `@listo @smoke` debe pasar siempre. Uno `@pendiente` no debe
ejecutarse todavía: se filtra con `--grep-invert` o el equivalente.

`@requiere-semilla` es distinto de `@pendiente`: el comportamiento **ya está
implementado y verificable**, pero su precondición exige insertar filas por SQL
porque el CRUD de ingresos, gastos y metas llega en las Fases 5 a 8. Cuando ese
CRUD exista, la etiqueta desaparece y solo se reescribe el `Dado`.

---

## Selectores

Los pasos describen comportamiento, no mecánica. Ningún escenario menciona un
selector: eso vive en las step definitions.

El puente entre ambos es [`../testids.md`](../testids.md), el catálogo de
`data-testid`. Se tratan como contrato: no se renombran sin anotarlo ahí.

---

## Datos de prueba

### Correos únicos

Cada registro exige un correo que no exista. La aplicación **no tiene endpoint
de borrado de usuarios**, así que los correos no se reciclan.

Recomendación: generar uno por ejecución, del estilo
`qa+{timestamp}@luma.app` o `qa+{uuid}@luma.app`.

### Semilla del presupuesto

Los escenarios `@requiere-semilla` necesitan ingresos, gastos o metas que hoy
solo se pueden crear por SQL. El script mínimo está en
[`06-ciclos-presupuestales.md`](06-ciclos-presupuestales.md#antes-de-empezar-las-plantillas-no-tienen-api-todavía).

Ojo con el orden: la semilla se inserta **antes** de abrir el ciclo. Un ciclo ya
materializado no cambia porque se agreguen plantillas después — eso es
precisamente la propiedad que el módulo garantiza.

### Limpieza — decisión pendiente

Hoy no hay forma de borrar un usuario de prueba desde la API. Las ejecuciones
repetidas acumulan filas en `users`.

Tres caminos posibles, en orden de preferencia:

1. **Un perfil de datos semilla** (`local`/`qa`) con usuarios fijos y conocidos,
   que se recrean al levantar el ambiente. Los escenarios de inicio de sesión
   usarían esos y no crearían nada. Con la Fase 4 este camino gana peso: un
   usuario semilla con ingresos, gastos y metas resolvería de una vez las
   precondiciones de todo el módulo de ciclos.
2. **Limpieza por SQL** entre ejecuciones:
   `DELETE FROM users WHERE email LIKE 'qa+%';`
   Las tablas del presupuesto se van en cascada con el usuario.
3. **Un endpoint de borrado** protegido y activo solo fuera de producción.

Es una decisión de producto, no de automatización. Cuando la tomes, se
implementa del lado de la aplicación.

### Reinicio completo del ambiente

```bash
docker compose down -v && docker compose up --build
```

Deja la base con el esquema de Flyway, el catálogo de categorías sembrado y cero
usuarios.

---

## Ambiente

| Qué | Dónde |
|---|---|
| Aplicación | http://localhost:8081 |
| Bandeja de correo (Mailpit) | http://localhost:8025 |
| API | http://localhost:8080/api/v1 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Salud | http://localhost:8080/actuator/health |

En desarrollo con `npm run dev` la aplicación queda en http://localhost:5173 y
habla con la API por el proxy de Vite.
