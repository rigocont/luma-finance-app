# LUMA

Aplicacion web de finanzas personales para quien vive de un sueldo fijo y necesita
saber una cosa: **cuanto dinero le queda realmente.**

LUMA no es un registro de gastos. Organiza el dinero en **ciclos presupuestales**
(quincenal, mensual o bimestral) y responde con cifras concretas:

- Cuanto tienes disponible este ciclo
- Que pagos vienen y cuales ya se vencieron
- Si te alcanza, si vas justo o si te falta
- Cuanto necesitas apartar para llegar a una meta
- Por que no alcanzo, a donde podria ir lo que sobra, y que categorias suben ciclo tras ciclo

---

## Estado actual

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Analisis y arquitectura | Completada |
| 1 | Foundation: monorepo, Docker, esquema, CI | Completada |
| 1.5 | Walking skeleton: registro, login, rutas protegidas | Completada |
| 2a | Sesion persistente: refresh token con rotacion | Completada |
| 2b | Recuperacion y cambio de contrasena | Completada |
| 3 | Sistema de diseno (tokens y tema, adelantados en la Fase 1) | Completada |
| 4a | Motor presupuestal: dominio puro y calculo | Completada |
| 4b | Ciclos: persistencia, materializacion y endpoints | Completada |
| 5a | Ingresos: CRUD y sincronizacion con el ciclo abierto | Completada |
| 5b | Ingresos: pantalla | Completada |
| 6 | Gastos fijos y variables: API y pantalla | Completada |
| 8a | Ahorros: metas, movimientos y aporte por ciclo | Completada |
| 8b | Ahorros: pantalla | Completada |
| 7 | Revision por ciclo: «Este ciclo», API y pantalla | Completada |
| 9 | Alta guiada: asistente inicial y limpieza de altas abandonadas | Completada |
| 10 | **Resumen financiero: balance, consejo de deficit y comparacion** | **Completada -> MVP** |
| 11 | Alertas: pago proximo, pago vencido, deficit y campana de notificaciones | Completada |
| 12 | Anuncios: espacio publicitario en el shell; monetizacion sin planes de pago | Completada |
| 13 | Analisis financiero (sin IA): causa del deficit, reparto del remanente y crecimiento sostenido | Completada -- la capa de redaccion con IA queda pendiente |
| 14 | Tour guiado de bienvenida: paso a paso para cuentas nuevas, omitible y repetible desde Ajustes | Pendiente |
| 15 | Interfaz bilingue (es/en): idioma del sistema por defecto, cambiable desde Ajustes; lo que la persona escribe nunca se traduce | Pendiente |

El plan completo esta en [`docs/00-arquitectura-fase-0.md`](docs/00-arquitectura-fase-0.md).
El plan de las apps moviles (iOS/Android) esta en [`docs/roadmap-mobile.md`](docs/roadmap-mobile.md).

> **Para retomar el proyecto:** empieza por
> [`docs/PUNTO-DE-RETORNO.md`](docs/PUNTO-DE-RETORNO.md). Trae el estado, las
> decisiones que sostienen el producto, como verificar, la deuda pendiente y los
> siguientes pasos.

**Lo que ya funciona:** la pila completa se levanta con un comando. Autenticacion
con sesion persistente y recuperacion de contrasena. El motor presupuestal calcula
ciclos quincenales, mensuales y bimestrales, materializa los renglones de un ciclo
a partir de ingresos, gastos y metas, y responde el balance por API. Ingresos,
gastos y metas de ahorro tienen pantalla propia, «Este ciclo» junta lo que falta
por definir antes de que el balance signifique algo, y el resumen responde la
pregunta con la que uno abre la aplicacion: cuanto me queda.

**El MVP esta completo.** Una cuenta nueva se configura con el asistente, abre
su ciclo, captura lo que entra y lo que sale, revisa lo que cambia, sigue sus
metas y ve en el resumen cuanto le queda — y, si no cierra, de donde podria
salir la diferencia. Todo desde la interfaz, sin Swagger.

---

## Arquitectura

```
  React + TypeScript + MUI          (SPA; mas adelante iOS/Android)
              |
              |  HTTPS / REST / JWT
              v
  Spring Boot  --  Monolito modular
              |
              v
          MySQL 8.4
```

Tres decisiones que gobiernan todo lo demas:

1. **Toda la logica financiera vive en el backend.** El frontend presenta cifras
   ya calculadas; nunca las deriva. Asi la aplicacion movil futura consume la
   misma API sin reimplementar nada.
2. **Monolito modular, no microservicios.** Los modulos estan separados por
   dominio de negocio con fronteras claras, dentro de un solo despliegue.
3. **El esquema lo gobierna Flyway.** La base se puede reconstruir desde cero con
   solo las migraciones. `ddl-auto` nunca pasa de `validate`.

Una cuarta, propia del dominio: **un ciclo cerrado es inmutable.** Sus renglones
son copias, no referencias, y ninguna edicion posterior los alcanza. Sin eso el
historial no sirve para nada.

---

## Tecnologias

**Backend** — Java 25 LTS, Spring Boot 4.1, Spring Data JPA, Spring Security,
Flyway, MySQL 8.4 LTS, springdoc-openapi, Actuator, ArchUnit

**Frontend** — React 19, TypeScript, Vite 8, Material UI v9, TanStack Query,
Zustand, React Router 7, Axios, Vitest

**Infraestructura** — Docker, Docker Compose, GitHub Actions, Mailpit

---

## Como ejecutar

### Todo con Docker (lo mas rapido)

```bash
cp .env.example .env
docker compose up --build
```

| Servicio | URL |
|---|---|
| Aplicacion | http://localhost:8081 |
| Bandeja de correo (Mailpit) | http://localhost:8025 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Salud | http://localhost:8080/actuator/health |
| MySQL | localhost:3306 |

Para empezar de cero (borra los datos):

```bash
docker compose down -v && docker compose up --build
```

### Desarrollo dia a dia

Base de datos en Docker, aplicacion en tu maquina — asi tienes recarga en caliente
en ambos lados.

```bash
# 1. Solo la base de datos
docker compose up mysql -d

# 2. Backend  (necesita JDK 25 y Maven)
cd backend
mvn spring-boot:run

# 3. Frontend (necesita Node 20.19+ o 22.12+; recomendado Node 24 LTS)
cd frontend
npm install
npm run dev
```

El frontend queda en http://localhost:5173 y habla con el backend a traves del
proxy de Vite, asi que en desarrollo no hay CORS que configurar.

### Pruebas del backend

```bash
cd backend
mvn -B test
```

Sin JDK ni Maven instalados, todo corre en un contenedor desde la raiz del
proyecto:

```powershell
.\scripts\verify.ps1              # backend: compila y corre las pruebas
.\scripts\verify.ps1 -Target all  # backend + frontend
.\scripts\verify.ps1 -SkipTests   # solo compilar
```

Deja `build-report.txt` (errores, pruebas y veredicto) y `build-full.log` (la
salida completa) en la raiz. Los dos estan en `.gitignore`.

---

## Variables de entorno

Copia `.env.example` a `.env`. Ningun secreto se versiona.

| Variable | Para que sirve | Valor por defecto |
|---|---|---|
| `MYSQL_DATABASE` | Nombre de la base | `luma` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | Credenciales de la aplicacion | `luma` / `luma` |
| `MYSQL_ROOT_PASSWORD` | Contrasena de root | `root` |
| `BACKEND_PORT` | Puerto de la API | `8080` |
| `FRONTEND_PORT` | Puerto de la aplicacion | `8081` |
| `SPRING_PROFILES_ACTIVE` | `local`, `dev`, `qa` o `prod` | `local` |
| `LUMA_CORS_ALLOWED_ORIGINS` | Origenes permitidos, separados por coma | `http://localhost:5173` |
| `VITE_API_BASE_URL` | URL de la API que consume el navegador | `/api/v1` |
| `VITE_ADSENSE_CLIENT_ID` | Cuenta de Google AdSense. Vacia en desarrollo | (vacio) |
| `VITE_ADSENSE_SLOT_ID` | Espacio de anuncio de esa cuenta. Vacio en desarrollo | (vacio) |

En produccion todos estos valores llegan por variables de entorno o GitHub
Secrets. Nunca por un archivo del repositorio.

---

## Estructura

```
luma/
  backend/          API de Spring Boot
    src/main/java/com/luma/
      auth/         Registro, login, tokens, recuperacion
      budget/       Ciclos, materializacion y motor de balance
      common/       Money, errores, paginacion, correlation id
      config/       Seguridad, CORS, OpenAPI, reloj
      expenses/     Gastos fijos y variables
      income/       Ingresos
      insights/     Analisis financiero sin IA (por ahora)
      notifications/ Alertas internas y envio de correo
      savings/      Metas de ahorro
      system/       Endpoint de diagnostico
      users/        Usuario y preferencias
    src/main/resources/
      db/migration/ Migraciones de Flyway
    src/test/java/   Unitarias, de dominio y de arquitectura
  frontend/         SPA de React
    src/
      app/          Bootstrap y providers
      components/   Layout y sistema de diseno
      features/     Estado y logica por funcionalidad
      lib/          Cliente de API, formato de dinero, testids
      pages/        Pantallas
      routes/       Rutas
      store/        Estado de cliente (Zustand)
      theme/        Tokens y tema de MUI
  infrastructure/   Docker y configuracion de ambientes
  docs/             Arquitectura, API, desarrollo, sistema de diseno, escenarios
  .github/workflows/ CI
```

Cada modulo de negocio repite la misma division interna: `api` (controladores y
DTOs), `application` (casos de uso), `domain` (reglas y entidades) e
`infrastructure` (repositorios). La dependencia va siempre en un sentido, y
ArchUnit lo verifica en cada build.

---

## Sobre las pruebas

El repositorio incluye **pruebas propias del desarrollo**: unitarias, de
integracion y de arquitectura (ArchUnit).

**No incluye, ni va a incluir, automatizacion E2E.** Nada de Playwright, Cypress,
Selenium, page objects, fixtures ni pipelines de E2E. Esa capa la desarrolla el
propietario del proyecto por separado.

Lo que el codigo si hace para facilitar esa automatizacion mas adelante:

- Mantiene atributos `data-testid` estables y documentados en
  [`docs/testids.md`](docs/testids.md). Se tratan como contrato y no se
  renombran a la ligera.
- Especifica el comportamiento esperado en Gherkin, alineado por funcionalidad,
  en [`docs/testing/`](docs/testing/README.md). Son documentos de
  especificacion: cada bloque es un `.feature` valido, listo para copiarse al
  proyecto de automatizacion sin traducir nada.

---

## Documentacion

| Documento | Contenido |
|---|---|
| [`docs/00-arquitectura-fase-0.md`](docs/00-arquitectura-fase-0.md) | Analisis completo, modelo de datos, decisiones y riesgos |
| [`docs/design-system.md`](docs/design-system.md) | Identidad visual, tokens y reglas |
| [`docs/development.md`](docs/development.md) | Flujo de trabajo, migraciones, convenciones |
| [`docs/api.md`](docs/api.md) | Convenciones de la API y reglas del presupuesto (el catalogo esta en Swagger) |
| [`docs/testids.md`](docs/testids.md) | Convencion de selectores |
| [`docs/testing/`](docs/testing/README.md) | Escenarios de prueba en Gherkin, por funcionalidad |
