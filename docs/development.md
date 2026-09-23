# Guia de desarrollo

## Requisitos

**Lo unico obligatorio es Docker.** El JDK, Maven, Node y MySQL viven dentro de
los contenedores, asi que no hace falta instalarlos en la maquina.

| Herramienta | Version | Cuando la necesitas |
|---|---|---|
| Docker Desktop | Con Compose v2 | Siempre. Desde la Fase 2, tambien para los tests de integracion |
| JDK | 25 LTS (Temurin) | Solo si corres el backend fuera de Docker |
| Maven | 3.9+ | Igual que arriba |
| Node | 20.19+ o 22.12+ (recomendado 24 LTS) | Solo si corres el frontend fuera de Docker. Requisito de Vite 8 |

### Instalar Docker Desktop en Windows

Requisitos del sistema: Windows 10 22H2 (build 19045) o Windows 11 23H2
(build 22631) o posterior, 64 bits, 8 GB de RAM y **virtualizacion activada en
BIOS**. Windows Home funciona: solo corre contenedores Linux, que es lo unico
que este proyecto usa.

```powershell
# 1. WSL 2 (PowerShell como administrador). Reiniciar despues.
wsl --install

# 2. Docker Desktop
winget install -e --id Docker.DockerDesktop

# 3. Abrir Docker Desktop y esperar a que el motor diga "Engine running".

# 4. En una ventana NUEVA de PowerShell (para que tome el PATH):
docker --version
docker compose version
```

**Si algo falla, casi siempre es una de estas tres:**

| Sintoma | Causa | Solucion |
|---|---|---|
| `docker` no se reconoce despues de instalar | La ventana de PowerShell tiene el PATH viejo | Cierra y abre una ventana nueva |
| Docker Desktop no arranca, menciona virtualizacion | Virtualizacion desactivada en BIOS | Verifica en Administrador de tareas > Rendimiento > CPU > Virtualizacion. Si dice "Deshabilitado", actívala en el BIOS (busca VT-x, AMD-V o SVM) |
| `wsl --install` falla | Version de Windows anterior a la soportada | Actualiza Windows |

**El primer `docker compose up --build` tarda varios minutos.** Descarga las
imagenes de MySQL, Maven y Node, y ademas resuelve todas las dependencias de
Maven y npm. No esta colgado. Los arranques siguientes son de segundos.

## Flujo tipico

```bash
docker compose up mysql -d      # base de datos
cd backend && mvn spring-boot:run
cd frontend && npm run dev
```

## Migraciones de base de datos

Todo cambio estructural pasa por Flyway. No hay excepciones.

**Nombre del archivo:** `V<n>__<descripcion_en_snake_case>.sql` en
`backend/src/main/resources/db/migration/`.

**Reglas:**

1. Una migracion **nunca** se edita despues de haberse fusionado a `main`.
   Flyway guarda un checksum; modificarla rompe cualquier base ya migrada.
2. Los cambios destructivos van en dos pasos y dos releases: primero agregar y
   migrar los datos, despues eliminar lo viejo.
3. La base debe poder reconstruirse desde cero. Se verifica con:
   `docker compose down -v && docker compose up`
4. Los enums se guardan como `VARCHAR`, no como `ENUM` de MySQL: agregar un valor
   no requiere `ALTER TABLE`.
5. Los importes son `DECIMAL(15,2)`. **Nunca `DOUBLE` ni `FLOAT`.**

## Convenciones de codigo

### Backend

- **Idioma:** el codigo en ingles, los mensajes de usuario en espanol.
- **Capas por modulo:** `api` -> `application` -> `domain` <- `infrastructure`.
  El dominio no importa Spring ni JPA. Lo verifica `ArchitectureTest`.
- **Inyeccion por constructor**, nunca por campo.
- **Dinero:** siempre `BigDecimal` via `common/model/Money`. Escala 2, redondeo
  `HALF_UP`. Un test de arquitectura falla si aparece un `double` o un `float`.
- **Fechas:** los instantes en UTC (`Instant`); las fechas de calendario como
  `LocalDate`, sin zona horaria.
- **Ownership:** ningun endpoint recibe un `userId` por parametro. El usuario
  siempre sale del token. Asi no existe la clase de vulnerabilidad donde un
  usuario lee los datos de otro cambiando un id en la URL.
- **Errores:** se lanza `ResourceNotFoundException` o `BusinessRuleException`.
  `GlobalExceptionHandler` los traduce; no se construyen respuestas de error
  a mano en los controladores.

### Frontend

- **Datos del servidor:** TanStack Query. **No** se copian a Zustand.
- **Estado de cliente:** Zustand, y solo para preferencias de interfaz.
- **Colores:** siempre desde `theme/tokens.ts`. Nunca un hex literal en un componente.
- **Dinero:** siempre `formatMoney` de `lib/money`. El lint avisa si aparece un
  `.toFixed()`.
- **Estados:** toda vista que carga datos maneja los cuatro casos — cargando,
  vacio, error y con datos. Existen componentes para los tres primeros.
- **Selectores:** cada elemento con el que se va a interactuar lleva
  `data-testid` tomado de `lib/testids.ts`. Ver `docs/testids.md`.

## Comandos

### Backend

```bash
mvn spring-boot:run                    # arrancar
mvn test                               # tests unitarios y de arquitectura
mvn verify                             # todo, incluido ArchUnit
mvn test -Dtest=MoneyTest              # un solo test
mvn clean package -DskipTests          # empaquetar
```

### Correo en desarrollo

Los correos que envia la aplicacion los captura **Mailpit**, que viene en el
compose. No se entrega nada a ningun destinatario real.

- Bandeja web: http://localhost:8025
- API (util para automatizar): http://localhost:8025/api/v1/messages

Ademas, en el perfil `local` el enlace de recuperacion se escribe en el log de
la aplicacion, para avanzar sin abrir la bandeja. Esa opcion
(`luma.mail.log-links`) esta apagada en produccion: un log con enlaces de
recuperacion es una via de acceso a las cuentas.

### Frontend

```bash
npm run dev            # servidor de desarrollo
npm run build          # build de produccion
npm run lint           # eslint
npm run typecheck      # tipos
npm test               # vitest
npm run format         # prettier
```

## Git

Rama por cambio, fusion a `main` por Pull Request. El CI debe pasar antes de fusionar.

```
feat(income): agregar endpoint de creacion de ingresos
fix(budget): corregir calculo de balance con gastos inactivos
docs(readme): actualizar instrucciones de Docker
chore(deps): subir Spring Boot a 4.1.2
```

## Definition of Done por fase

- Compila
- Arranca con `docker compose up`
- Las migraciones aplican desde una base vacia
- No rompe nada de las fases anteriores
- La logica nueva tiene tests unitarios
- Swagger refleja los endpoints nuevos
- Los `data-testid` nuevos estan en `lib/testids.ts` y en `docs/testids.md`
- La documentacion afectada esta actualizada
