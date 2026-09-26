# LUMA movil

App de React Native (Expo) para iOS. Ver [`docs/roadmap-mobile.md`](../docs/roadmap-mobile.md)
para el plan completo (M0-M5) y las decisiones de arquitectura.

**Estado:** M0 — scaffold, navegacion base y wiring de autenticacion.

## Arrancar

```bash
cp .env.example .env.local   # ajusta EXPO_PUBLIC_API_BASE_URL si tu backend no esta en localhost
npm install
npm start
```

Necesitas el backend de LUMA corriendo (`../backend`) para que login/registro funcionen: esta
app no trae datos de ejemplo ni un modo sin conexion.

## Estructura

```
src/
├── app/              rutas de expo-router (un archivo = una pantalla)
│   ├── _layout.tsx     layout raiz: decide (auth) vs (app) segun haya sesion
│   ├── (auth)/         login, registro
│   └── (app)/          pantallas detras de sesion (M0: solo un placeholder)
├── features/
│   └── auth/          store de sesion, servicio de auth, bootstrap al abrir la app
├── lib/
│   ├── api/            cliente HTTP (axios) y tipos de error de la API
│   └── storage/        wrapper de expo-secure-store para el refresh token
└── theme/              subconjunto de los tokens de docs/design-system.md
```

## Autenticacion

Mismo contrato que el frontend web (`../frontend/src/features/auth`), sin cambios de backend:

- Cada peticion manda la cabecera `X-Client-Type: mobile`, asi el backend devuelve el refresh
  token en el cuerpo de la respuesta en vez de una cookie (un cliente nativo no tiene donde
  recibirla).
- El refresh token se guarda con `expo-secure-store` (lo unico que sobrevive a cerrar la app).
  El access token vive solo en memoria, igual que en el frontend web.
- Al abrir la app, `useBootstrapSession` intenta renovar la sesion con el refresh token
  guardado antes de decidir si mostrar el login o la app.

## Comandos

```bash
npm run typecheck   # tsc --noEmit
npm run lint        # eslint .
npm run format      # prettier --write
```

No hay pruebas automatizadas todavia (llegan con paridad de pantallas reales en M1+).
