# Convencion de selectores

Este documento existe para una sola cosa: que la automatizacion de pruebas tenga
selectores **estables** contra los cuales trabajar.

Aqui no hay pruebas. La automatizacion se desarrolla por separado.

---

## La regla

Todo elemento con el que se pueda interactuar o cuyo contenido haya que verificar
lleva un atributo `data-testid`.

```
<feature>-<elemento>-<rol>
```

En minusculas, separado por guiones.

```html
<button data-testid="income-create-button">
<input  data-testid="income-form-amount-input">
<tr     data-testid="income-table-row" data-testid-id="a3f1...">
<div    data-testid="budget-balance-card">
```

## De donde salen

De un solo archivo: [`frontend/src/lib/testids.ts`](../frontend/src/lib/testids.ts).

Ningun componente escribe un `data-testid` literal. Siempre se importa del catalogo.

```tsx
import { testIds } from '@/lib/testids';

<Button data-testid={testIds.dashboard.connectionCard}>
```

Asi el catalogo completo se lee en un archivo y renombrar algo tiene un solo
lugar donde hacerse.

## Son contrato

**Los `data-testid` se tratan como API publica de la interfaz.**

- No se renombran sin anotarlo en este documento
- No se eliminan mientras el elemento siga existiendo
- No dependen del texto visible, del idioma, ni de la posicion en el DOM
- No cambian al reestructurar el layout

Un `data-testid` que cambia con cada refactor no sirve para automatizar nada.

## Elementos que se repiten

Cuando hay varias instancias del mismo elemento, el `data-testid` se mantiene
igual y el identificador va en un atributo aparte:

```html
<tr data-testid="income-table-row" data-testid-id="a3f1-...">
```

Asi se puede seleccionar el conjunto (todas las filas) o uno concreto (la fila de
ese ingreso), sin depender del indice.

## Las pantallas se cargan al visitarlas

Cada seccion viaja en su propio trozo de codigo y se descarga la primera vez que
se entra. Entre el clic y el contenido aparece `state-loading` por un instante,
incluso en pantallas que ya tienen sus datos en cache.

Para la automatizacion eso significa **esperar al elemento, no al clic**. Un
escenario que afirme "no hay estado de carga" justo despues de navegar va a
fallar de forma intermitente, y la culpa no sera de la aplicacion.

---

## Estados

Los estados de carga, vacio y error tienen `data-testid` propios, porque
distinguirlos es exactamente lo que una prueba necesita verificar:

| Estado | `data-testid` |
|---|---|
| Cargando | `state-loading` |
| Vacio | `state-empty` |
| Error | `state-error` |
| Reintentar | `state-error-retry` |

---

## Catalogo actual

### Sesion

| Elemento | `data-testid` |
|---|---|
| Pagina de inicio de sesion | `auth-login-page` |
| Pagina de registro | `auth-register-page` |
| Campo nombre | `auth-name-input` |
| Campo correo | `auth-email-input` |
| Campo contrasena | `auth-password-input` |
| Boton de envio | `auth-submit-button` |
| Error general del formulario | `auth-form-error` |
| Enlace a registro | `auth-go-to-register` |
| Enlace a inicio de sesion | `auth-go-to-login` |
| Nombre del usuario en el encabezado | `auth-user-name` |
| Cerrar sesion | `auth-logout-button` |
| Pantalla de arranque | `auth-boot-splash` |
| Pagina de recuperacion | `auth-forgot-password-page` |
| Confirmacion de enlace enviado | `auth-reset-link-sent` |
| Pagina de contrasena nueva | `auth-reset-password-page` |
| Enlace invalido | `auth-invalid-reset-link` |
| Pedir un enlace nuevo | `auth-request-new-link` |
| Enlace a recuperacion | `auth-go-to-forgot-password` |
| Campo contrasena actual | `auth-current-password-input` |
| Campo contrasena nueva | `auth-new-password-input` |
| Campo confirmar contrasena | `auth-confirm-password-input` |
| Aviso en el inicio de sesion | `auth-login-notice` |

### Ajustes

| Elemento | `data-testid` |
|---|---|
| Pagina | `settings-page` |
| Correo de la cuenta | `settings-email` |
| Tarjeta de estado del sistema | `settings-system-card` |
| Estado de la conexion | `settings-system-status` |
| Version de la API | `settings-system-version` |
| Tarjeta del ciclo | `settings-cycle-card` |
| Tipo de ciclo | `settings-cycle-type` |
| Dia de anclaje | `settings-cycle-anchor` |
| Guardar el ciclo | `settings-cycle-submit` |
| Error del ciclo | `settings-cycle-error` |
| Tarjeta de apariencia | `settings-theme-card` |
| Opcion de tema | `settings-theme-light`, `settings-theme-dark`, `settings-theme-system` |
| Tarjeta de contrasena | `settings-change-password-card` |
| Boton de cambiar contrasena | `settings-change-password-submit` |
| Error del formulario | `settings-change-password-error` |

Los errores por campo NO tienen `data-testid` propio: aparecen como texto de
ayuda del input correspondiente, asi que se leen desde el campo.

### Layout

| Elemento | `data-testid` |
|---|---|
| Contenedor principal | `layout-app-shell` |
| Barra lateral | `layout-sidebar` |
| Boton de menu (movil) | `layout-sidebar-toggle` |
| Encabezado | `layout-header` |
| Cambio de tema | `layout-theme-toggle` |
| Elemento de navegacion | `layout-nav-<key>` |
| Insignia de pendientes | `layout-nav-badge-<key>` |
| Titulo de la pagina | `layout-page-title` |

Claves de navegacion: `dashboard`, `current-cycle`, `incomes`, `expenses`,
`savings`, `settings`.

La insignia solo existe cuando hay algo pendiente, y hoy solo la lleva
`current-cycle`. Un escenario que la busque con la bandeja vacia no la va a
encontrar, y eso es correcto: un contador en cero no se dibuja.

### Alertas

| Elemento | `data-testid` |
|---|---|
| Boton de la campana | `notifications-bell` |
| Insignia con el conteo | `notifications-badge` |
| Menu desplegable | `notifications-menu` |
| Lista de alertas | `notifications-list` |
| Una alerta (por id) | `notifications-row-<id>` |
| Bandeja vacia | `notifications-empty` |
| Marcar todas como leidas | `notifications-mark-all-read` |

Dos cosas que evitan escenarios fragiles:

`notifications-list` se dibuja siempre, vacia o no; `notifications-empty` se
agrega ademas cuando no hay alertas. Un escenario que busque la bandeja vacia
comprueba `notifications-empty`, no la ausencia de `notifications-list`.

`notifications-mark-all-read` **solo existe con alertas sin leer**. Con todo
leido el boton no se dibuja, y no es un fallo.

No hay pantalla propia para las alertas en esta fase: cada fila manda a "Este
ciclo" (`layout-nav-current-cycle`), que es donde las tres condiciones se
resuelven de verdad. Un escenario que busque una ruta como `/notifications` no
va a encontrar nada.

### Anuncios

| Elemento | `data-testid` |
|---|---|
| Espacio real (cuenta configurada) | `ads-slot` |
| Marcador (sin cuenta configurada) | `ads-placeholder` |

Uno de los dos existe, nunca ambos: sin `VITE_ADSENSE_CLIENT_ID` y
`VITE_ADSENSE_SLOT_ID` -el caso normal en desarrollo- se dibuja el marcador;
con ambas, el espacio real. Un escenario de este entorno siempre encuentra
`ads-placeholder`.

El mismo espacio aparece al final de las seis pantallas dentro del shell
(resumen, este ciclo, ingresos, gastos, ahorros, ajustes). No aparece en la
alta guiada ni en las pantallas de sesion, que viven fuera de `AppShell`.

### Resumen financiero

| Elemento | `data-testid` |
|---|---|
| Pagina | `dashboard-page` |
| Saludo | `dashboard-greeting` |
| Rango del ciclo | `dashboard-cycle-range` |
| Abrir el primer ciclo | `dashboard-open-cycle` |
| Cerrar el ciclo | `dashboard-close-cycle` |
| Bloque de la cifra | `dashboard-hero` |
| La cifra | `dashboard-hero-amount` |
| El titular | `dashboard-hero-headline` |
| La frase que explica | `dashboard-hero-detail` |
| Aviso de estimacion | `dashboard-hero-estimate-notice` |
| Tarjeta del reparto | `dashboard-composition` |
| La barra | `dashboard-composition-bar` |
| Segmento (por clave) | `dashboard-composition-{fixed\|variable\|savings}` |
| Total de gastos fijos | `dashboard-total-fixed` |
| Total de gastos variables | `dashboard-total-variable` |
| Total de ahorro | `dashboard-total-savings` |
| Disponible / lo que no cabe | `dashboard-total-income` |
| Proporcion de ahorro | `dashboard-savings-rate` |
| Tarjeta de lo que sigue | `dashboard-upcoming` |
| Renglon por pagar | `dashboard-upcoming-row` |
| Marca de vencido | `dashboard-upcoming-overdue` |
| Sin pendientes | `dashboard-upcoming-empty` |
| Accion "Ya lo pague" | `dashboard-settle-action` |
| Tarjeta del consejo | `dashboard-advice` |
| Renglon propuesto | `dashboard-advice-row` |
| Aviso de que no alcanza | `dashboard-advice-shortfall` |
| Quitar del ciclo | `dashboard-advice-skip` |
| Tarjeta de comparacion | `dashboard-trend` |
| La grafica | `dashboard-trend-chart` |
| La frase de la diferencia | `dashboard-trend-delta` |
| Sin con que comparar | `dashboard-trend-empty` |
| Tarjeta de analisis financiero | `dashboard-insights` |
| Bloque de causa del deficit | `dashboard-insights-deficit-cause` |
| Bloque del reparto del remanente | `dashboard-insights-surplus` |
| Meta del reparto (por fila) | `dashboard-insights-surplus-row` |
| Bloque de crecimiento sostenido | `dashboard-insights-growth` |
| Categoria en racha (por fila) | `dashboard-insights-growth-row` |

Cuatro cosas que evitan escenarios fragiles:

`dashboard-advice` **solo existe con deficit**. Con el ciclo en orden la tarjeta
no se dibuja, y no es un fallo.

`dashboard-insights` **solo existe si hay al menos una senal que mostrar**: una
causa de deficit, un reparto de remanente, o alguna categoria con crecimiento
sostenido. Un ciclo balanceado y sin historial suficiente no dibuja la
tarjeta, y no es un fallo.

`dashboard-hero-estimate-notice` solo aparece cuando hay gastos sin confirmar.

En la grafica, **no afirmes sobre el color de las barras**. El signo lo dice la
posicion respecto al cero y lo confirma la etiqueta; el verde y el rojo son
refuerzo, porque esa pareja falla la comprobacion de daltonismo. Un escenario que
verifique el color estaria verificando la decoracion.

### Ingresos

| Elemento | `data-testid` |
|---|---|
| Pagina | `incomes-page` |
| Boton capturar | `incomes-create-button` |
| Tabla | `incomes-list` |
| Fila (por id) | `incomes-row-{id}` |
| Nombre de la fila | `incomes-row-name` |
| Monto de la fila | `incomes-row-amount` |
| Tipo de la fila | `incomes-row-type` |
| Calendario de la fila | `incomes-row-schedule` |
| Marca «Sin contar» | `incomes-row-inactive` |
| Marca «Pide revision» | `incomes-row-review` |
| Menu de la fila | `incomes-row-menu` |
| Accion editar | `incomes-action-edit` |
| Accion contar / dejar de contar | `incomes-action-toggle-active` |
| Accion eliminar | `incomes-action-delete` |
| Filtro por tipo | `incomes-filter-type` |
| Filtro por estado | `incomes-filter-active` |
| Selector de orden | `incomes-sort` |
| Paginacion | `incomes-pagination` |
| Cajon del formulario | `incomes-drawer` |
| Titulo del cajon | `incomes-drawer-title` |
| Campo nombre | `incomes-name-input` |
| Campo tipo | `incomes-type-input` |
| Campo monto | `incomes-amount-input` |
| Campo frecuencia | `incomes-frequency-input` |
| Campo dia del mes | `incomes-expected-day-input` |
| Campo desde | `incomes-start-date-input` |
| Campo hasta | `incomes-end-date-input` |
| Campo notas | `incomes-notes-input` |
| Aviso de monto variable | `incomes-review-notice` |
| Guardar | `incomes-submit-button` |
| Cancelar | `incomes-cancel-button` |
| Error general del formulario | `incomes-form-error` |

**El id de la fila lleva el UUID del ingreso**, no su posicion. Una fila
identificada por indice cambia de significado en cuanto se reordena la lista, y
una prueba que dependa de eso falla sin que nada este roto.

### Gastos

Fijos y variables comparten pantalla: solo los distingue el campo «El monto».

| Elemento | `data-testid` |
|---|---|
| Pagina | `expenses-page` |
| Boton capturar | `expenses-create-button` |
| Tabla | `expenses-list` |
| Fila (por id) | `expenses-row-{id}` |
| Nombre de la fila | `expenses-row-name` |
| Categoria de la fila | `expenses-row-category` |
| Calendario de la fila | `expenses-row-schedule` |
| Monto de la fila | `expenses-row-amount` |
| Marca de flexibilidad | `expenses-row-flexibility` |
| Marca «Sin contar» | `expenses-row-inactive` |
| Marca «Pide revision» | `expenses-row-review` |
| Menu de la fila | `expenses-row-menu` |
| Accion editar | `expenses-action-edit` |
| Accion contar / dejar de contar | `expenses-action-toggle-active` |
| Accion eliminar | `expenses-action-delete` |
| Filtro por tipo de monto | `expenses-filter-kind` |
| Filtro por categoria | `expenses-filter-category` |
| Filtro por estado | `expenses-filter-active` |
| Selector de orden | `expenses-sort` |
| Paginacion | `expenses-pagination` |
| Cajon del formulario | `expenses-drawer` |
| Titulo del cajon | `expenses-drawer-title` |
| Campo nombre | `expenses-name-input` |
| Campo categoria | `expenses-category-input` |
| Campo tipo de monto | `expenses-kind-input` |
| Campo monto | `expenses-amount-input` |
| Campo flexibilidad | `expenses-flexibility-input` |
| Campo frecuencia | `expenses-frequency-input` |
| Campo dia de pago | `expenses-due-day-input` |
| Campo desde | `expenses-start-date-input` |
| Campo hasta | `expenses-end-date-input` |
| Campo notas | `expenses-notes-input` |
| Aviso de monto variable | `expenses-review-notice` |
| Aviso de gasto critico | `expenses-critical-notice` |
| Guardar | `expenses-submit-button` |
| Cancelar | `expenses-cancel-button` |
| Error general del formulario | `expenses-form-error` |

### Alta guiada

El asistente vive fuera del shell: no hay `layout-sidebar` ni `layout-header` en
esta pantalla, y un escenario que los busque aqui no los va a encontrar.

| Elemento | `data-testid` |
|---|---|
| Pagina | `onboarding-page` |
| Barra de pasos (solo en pantalla ancha) | `onboarding-stepper` |
| Contenido del paso | `onboarding-step-{cycle\|incomes\|expenses\|savings\|summary}` |
| Continuar | `onboarding-next` |
| Atras | `onboarding-back` |
| Saltar este paso | `onboarding-skip-step` |
| Hacerlo despues | `onboarding-skip-all` |
| Empezar | `onboarding-finish` |
| Error | `onboarding-error` |
| Tipo de ciclo | `onboarding-cycle-type` |
| Dia de anclaje | `onboarding-cycle-anchor` |
| Aviso del paso de ciclo | `onboarding-cycle-preview` |
| Nombre del ingreso | `onboarding-income-name` |
| Monto del ingreso | `onboarding-income-amount` |
| Tipo del ingreso | `onboarding-income-type` |
| Frecuencia del ingreso | `onboarding-income-frequency` |
| Dia del ingreso | `onboarding-income-day` |
| Agregar ingreso | `onboarding-income-add` |
| Lista de ingresos | `onboarding-income-list` |
| Renglon de ingreso | `onboarding-income-row` |
| Categoria del catalogo (por codigo) | `onboarding-category-{CODE}` |
| Nombre del gasto | `onboarding-expense-custom-name` |
| Monto del gasto | `onboarding-expense-amount` |
| Lista de gastos | `onboarding-expense-list` |
| Renglon de gasto | `onboarding-expense-row` |
| Nombre de la meta | `onboarding-goal-name` |
| Objetivo de la meta | `onboarding-goal-target` |
| Aporte por ciclo | `onboarding-goal-per-cycle` |
| Agregar meta | `onboarding-goal-add` |
| Lista de metas | `onboarding-goal-list` |
| Resumen: ingresos | `onboarding-summary-incomes` |
| Resumen: gastos | `onboarding-summary-expenses` |
| Resumen: metas | `onboarding-summary-goals` |
| Resumen: ciclo | `onboarding-summary-cycle` |

`onboarding-skip-step` solo existe en los pasos opcionales —gastos y metas—, y
`onboarding-back` no existe en el primero. `onboarding-stepper` esta oculto en
telefono, donde el avance se dice con texto.

Los codigos de categoria son los de la semilla: `HOUSING`, `UTILITIES`,
`GROCERIES`, `OTHER` y los demas. Son estables: viven en la migracion, no en la
interfaz.

### Revision del ciclo

La seccion «Este ciclo». Los renglones son tarjetas con un campo de monto cada
una: se captura todo y se confirma de una vez.

| Elemento | `data-testid` |
|---|---|
| Pagina | `review-page` |
| Rango del ciclo | `review-cycle-range` |
| Lista | `review-list` |
| Tarjeta (por id) | `review-row-{id}` |
| Nombre | `review-row-name` |
| Lo estimado | `review-row-planned` |
| Campo de monto | `review-row-amount-input` |
| Sugerencia del ciclo pasado | `review-row-suggestion` |
| Boton usar la sugerencia | `review-row-use-suggestion` |
| Abrir el historial | `review-row-history` |
| Confirmar todo | `review-confirm-all` |
| Cuantos faltan | `review-confirmed-count` |
| Error del lote | `review-form-error` |
| Dialogo de historial | `review-history-dialog` |
| Renglon del historial | `review-history-row` |
| Promedio | `review-history-average` |
| Bloque de ahorros | `review-savings-section` |
| Renglon de ahorro (por id) | `review-savings-row-{id}` |
| Registrar aporte | `review-savings-settle` |
| Dialogo de registro | `review-settle-dialog` |
| Campo de monto apartado | `review-settle-amount` |
| Campo de fecha | `review-settle-date` |
| Casilla «No registrarlo en la meta» | `review-settle-skip-goal` |
| Guardar | `review-settle-submit` |
| Error del registro | `review-settle-error` |

`review-row-use-suggestion` solo existe cuando el campo NO tiene ya el monto
sugerido: un boton que no haria nada no se dibuja. Un escenario que lo busque
siempre va a fallar la primera vez que abra la pantalla.

`review-row-suggestion` no aparece si el gasto no tiene historia. En ese caso el
campo viene con la estimacion, no vacio.

### Ahorros

Las metas se presentan como tarjetas reordenables, no como tabla: el orden es
dato, y una tabla no se arrastra bien.

| Elemento | `data-testid` |
|---|---|
| Pagina | `savings-page` |
| Boton crear meta | `savings-create-button` |
| Lista | `savings-list` |
| Tarjeta (por id) | `savings-card-{id}` |
| Nombre de la tarjeta | `savings-card-name` |
| Porcentaje | `savings-card-progress` |
| Barra de progreso | `savings-card-progress-bar` |
| Lo juntado | `savings-card-saved` |
| Lo que falta | `savings-card-remaining` |
| Aporte por ciclo o fecha objetivo | `savings-card-per-cycle` |
| Marca de estado (alcanzada / pausada) | `savings-card-status` |
| Menu de la tarjeta | `savings-card-menu` |
| Asa de arrastre | `savings-drag-handle` |
| Subir prioridad | `savings-move-up` |
| Bajar prioridad | `savings-move-down` |
| Accion editar | `savings-action-edit` |
| Accion registrar aportacion | `savings-action-contribute` |
| Accion registrar retiro | `savings-action-withdraw` |
| Accion pausar / reanudar | `savings-action-pause` |
| Accion ver movimientos | `savings-action-movements` |
| Accion eliminar | `savings-action-delete` |
| Filtro por estado | `savings-filter-status` |
| Cajon del formulario | `savings-drawer` |
| Titulo del cajon | `savings-drawer-title` |
| Campo nombre | `savings-name-input` |
| Campo monto objetivo | `savings-target-input` |
| Campo modo de aporte | `savings-mode-input` |
| Campo fecha objetivo | `savings-target-date-input` |
| Campo aporte por ciclo | `savings-per-cycle-input` |
| Guardar | `savings-submit-button` |
| Cancelar | `savings-cancel-button` |
| Error general del formulario | `savings-form-error` |
| Dialogo de movimiento | `savings-movement-dialog` |
| Campo monto del movimiento | `savings-movement-amount` |
| Campo fecha del movimiento | `savings-movement-date` |
| Campo notas del movimiento | `savings-movement-notes` |
| Guardar movimiento | `savings-movement-submit` |
| Error del movimiento | `savings-movement-error` |
| Cajon de movimientos | `savings-movements-drawer` |
| Renglon del historial | `savings-movement-row` |
| Historial vacio | `savings-movements-empty` |

`savings-move-up` y `savings-move-down` no son una comodidad: el arrastre nativo
de HTML5 no funciona en pantallas tactiles, asi que en telefono son la UNICA
forma de reordenar. Un escenario que solo arrastra no cubre el caso movil.

`savings-card-per-cycle` cambia de contenido segun el modo: con aporte fijo dice
el monto por ciclo, con fecha objetivo dice la fecha. Con modo manual no aparece.

### Avisos y confirmaciones

Son transversales: los usa cualquier pantalla.

| Elemento | `data-testid` |
|---|---|
| Aviso efimero (toast) | `toast-root` |
| Dialogo de confirmacion | `confirm-dialog` |
| Confirmar | `confirm-accept` |
| Cancelar | `confirm-cancel` |

---

## Donde va el atributo en un campo de MUI

No es arbitrario, y conviene saberlo al escribir los selectores:

| Tipo de campo | Donde queda el `data-testid` |
|---|---|
| Texto, numero, fecha, area de texto | En el `<input>` / `<textarea>`, via `slotProps.htmlInput` |
| Desplegable (`select`) | En la raiz del campo |

La razon es que un desplegable de MUI esconde su `<input>` nativo: poner ahi el
atributo daria un elemento invisible con el que no se puede interactuar.

---

## Escenarios que la aplicacion va a ofrecer

Para referencia al planear la automatizacion, y para que cada fase los construya
a proposito:

| Fase | Escenarios disponibles |
|---|---|
| 1 | Navegacion SPA, responsive, cambio de tema, estados de carga y error |
| 1.5 | Formularios con validacion del servidor, redireccion despues de login, rutas protegidas, sesion perdida al recargar |
| 2 | Refresh token, expiracion y renovacion de sesion, recuperacion de contrasena |
| 3 | Modales, confirmaciones, toasts, drawers, tooltips |
| 4 | Cambios de estado asincronos, listas que se regeneran por ciclo |
| 5-8 | CRUD completo, tablas dinamicas, filtros, orden, busqueda, paginacion, confirmacion de borrado |
| 7 | Formularios de filas repetibles (revision de gastos variables) |
| 8 | Barras de progreso, drag and drop para reordenar metas |
| 9 | Wizard multi-paso con estado persistente |
| 10 | Datos que cambian segun las acciones previas del usuario |

Ninguno de estos escenarios existe para dificultar las pruebas: todos tienen
sentido funcional dentro de la aplicacion.
