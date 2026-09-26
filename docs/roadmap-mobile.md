# Roadmap movil de LUMA

> Este documento es el plan de las apps moviles (iOS primero, Android despues).
> Vive aparte de la tabla de fases de [`README.md`](../README.md) porque tiene
> su propio ritmo y no compite por numero de fase con el producto web: el web
> sigue su tabla (Fase 0-15), lo movil sigue esta.

## 1. Decision: stack y estructura de repositorio

**Stack: React Native con Expo.**

- Un solo codigo fuente para iOS y Android — la prioridad de la persona es
  iOS primero (tiene Mac + iPhone), pero sin duplicar trabajo cuando le toque
  a Android.
- Reutiliza el TypeScript que ya se conoce del frontend web (tipos, forma de
  pensar los datos, gran parte del criterio de UI), aunque los componentes
  visuales no se comparten 1:1 con MUI — RN tiene su propio sistema de vistas.
- Expo simplifica el empaquetado y la firma para TestFlight (iOS) y para la
  Play Store (Android) mas adelante, sin tener que mantener proyectos nativos
  Xcode/Android Studio a mano desde el primer dia (se puede "eyectar" despues
  si hace falta algo que Expo no cubre).

**Estructura: mismo repositorio, carpeta nueva.**

```
LUMA/
├── backend/          (sin cambios)
├── frontend/         (sin cambios)
├── mobile/           <- nuevo: proyecto Expo/React Native
├── infrastructure/
├── docs/
│   └── roadmap-mobile.md   <- este archivo
└── scripts/
```

Se descarto un repositorio separado porque lo movil consume la misma API y
depende de las mismas decisiones de dominio (ciclos, categorias, metas) que
ya viven documentadas en `docs/00-arquitectura-fase-0.md`; separar el
repositorio hubiera significado sincronizar dos fuentes de verdad a mano.
Con una carpeta dentro del mismo monorepo, un cambio de contrato en el
backend y su ajuste correspondiente en movil quedan en el mismo commit.

## 2. Por que esto no arranca de cero

La arquitectura original (`docs/00-arquitectura-fase-0.md`, seccion 1.7,
pregunta resuelta P9) ya decidio un esquema de autenticacion hibrido
pensando en un cliente movil futuro, y **ese esquema ya esta implementado**,
no solo planeado:

- `AuthController` (`backend/src/main/java/com/luma/auth/api/AuthController.java`)
  lee una cabecera `X-Client-Type: mobile`. Un cliente web recibe el refresh
  token en una cookie `HttpOnly` (como hasta ahora); un cliente que se
  identifica como movil lo recibe en el cuerpo de la respuesta, porque un
  dispositivo movil no maneja cookies de navegador de la misma forma.
- El access token en ambos casos vive solo en memoria del lado del cliente.

En la practica esto quiere decir que **el backend no necesita ningun cambio
para que arranque el trabajo movil**: la app de React Native manda
`X-Client-Type: mobile` en login y refresh, guarda el refresh token con
almacenamiento seguro del dispositivo (`expo-secure-store`), y todo lo demas
— ciclos, ingresos, gastos, metas, alertas, resumen — es la misma API REST
que ya consume el frontend web.

## 3. Fases

| Fase | Contenido | Nota |
|---|---|---|
| M0 | Scaffold de Expo, navegacion base, wiring de autenticacion (`X-Client-Type: mobile`, secure storage del refresh token, cliente HTTP compartido de forma conceptual con el frontend web) | Sin esto no hay nada mas que construir |
| M1 | Paridad de solo lectura: login, «Este ciclo» y el resumen financiero en modo lectura | Primer build que se siente como LUMA en el telefono |
| M2 | Pantallas CRUD nucleo: ingresos, gastos, metas de ahorro | Mismo alcance funcional que el frontend web, adaptado a movil |
| M3 | Revision por ciclo, alertas internas, y notificaciones push nativas via APNs | Las notificaciones push son una ventaja real de movil sobre web: un pago vencido puede avisar sin abrir la app |
| M4 | Beta cerrada por TestFlight | Iteracion con uso real en el iPhone de la persona antes de publicar |
| M5 | Puerto a Android sobre el mismo codigo RN, beta interna en Play Store | Se hace despues de validar iOS, no en paralelo |

No hay fecha objetivo todavia; el orden es lo que importa, no el calendario.

## 4. CI

No se agrega un workflow de CI para `mobile/` (por ejemplo
`.github/workflows/mobile-ci.yml`) hasta que exista codigo real que probar.
Un job de CI apuntando a una carpeta vacia solo queda permanentemente en rojo
o vacio sin avisar nada; se crea junto con el primer commit de M0.

## 5. Que decide esta version del documento

- Stack: React Native / Expo.
- Repositorio: mismo repo, carpeta `mobile/` nueva.
- Orden: iOS primero (M0-M4), Android despues (M5) sobre el mismo codigo.
- Cero cambios de backend requeridos para empezar M0.

Lo que todavia no se decide (a proposito, hasta que M0 este mas cerca):
nombre exacto de la app en las tiendas, cuenta de desarrollador de Apple a
usar, y si el diseno visual de movil replica 1:1 los tokens de
`docs/design-system.md` o adapta algunos (los patrones tactiles de iOS no
siempre coinciden con los de una SPA de escritorio/web).
