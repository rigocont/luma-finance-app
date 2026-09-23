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

## Catalogo actual (Fase 1.5)

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
| Titulo de la pagina | `layout-page-title` |

Claves de navegacion: `dashboard`, `incomes`, `fixed-expenses`,
`variable-expenses`, `savings`, `settings`.

### Dashboard

| Elemento | `data-testid` |
|---|---|
| Pagina | `dashboard-page` |
| Tarjeta de conexion | `dashboard-connection-card` |
| Indicador de estado | `dashboard-connection-status` |
| Version de la API | `dashboard-api-version` |
| Saludo con el correo de la sesion | `dashboard-greeting` |

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
