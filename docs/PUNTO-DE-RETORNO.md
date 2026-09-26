# Punto de retorno

**Este archivo es lo primero que hay que leer para retomar el proyecto.**

Estado al 26 de septiembre de 2026: **el MVP está terminado.** Diez fases, 298
pruebas de backend y 8 de frontend en verde, todo el flujo operable desde la
interfaz sin tocar Swagger.

Si vienes de una sesión anterior de Claude: no hace falta reconstruir el
contexto leyendo el código. Lee este archivo completo, después la sección
«Cómo verificar», y con eso puedes trabajar.

---

## 0. Antes que nada: hay trabajo sin comitear

Al cerrar esta sesión había **101 archivos sin comitear**, incluyendo las fases
7, 8, 9 y 10 completas. El último commit del repositorio es `c4ca40f add
package-lock`, que es de la época de la Fase 6.

**Primero commitea, después trabaja.** Y de paso saca del control de versiones
el reporte de verificación, que se colo en un commit anterior:

```powershell
git rm --cached build-report.txt
git add -A
git commit -m "feat: fases 7 a 10 — revision por ciclo, ahorros, alta guiada y resumen financiero"
```

El `.gitignore` ya tiene las entradas para `build-report.txt` y para la carpeta
`Claude outputs/`, que es basura de entrega temporal; sus cuatro archivos ya
viven en `.github/workflows/`.

---

## 1. Qué es LUMA y qué constriñe el trabajo

Aplicación web de finanzas personales. Backend Spring Boot, frontend React,
MySQL. La especificación es un «PROMPT MAESTRO» con secciones numeradas que el
propietario del proyecto (Rigo) tiene aparte.

**Las reglas que no se negocian**, porque vienen de ahí:

**§3 — NO se crea automatización de pruebas.** Ni Playwright, ni Cypress, ni
Selenium, ni page objects, ni fixtures, ni runners, ni pipelines E2E. Cero.
El propietario la desarrolla él, en una etapa independiente. Se respetó de punta
a punta: no hay un solo archivo de eso en el repositorio, y los flujos de CI
llevan un comentario que lo dice para que nadie lo agregue «de buena fe».

**§4 — La aplicación SÍ lleva elementos dinámicos** para practicar automatización
después: modales, toasts, cajones, menús, pestañas, formularios de varios pasos,
arrastrar y soltar, estados de carga/vacío/error. Todos con `data-testid`
estable. Pero **sin complejidad artificial** solo para dificultar las pruebas.

**§7 y §14 — Nunca se exponen ni se versionan** contraseñas, tokens, secretos,
trazas de error ni llaves. Configuración por variable de entorno.

**§21 — Pruebas permitidas:** unitarias, de integración, de servicio, de
repositorio y de API. Prohibidas: las de §3.

**§24 — Monolito modular.** «Simple, Modular, Escalable», en ese orden.
**Preguntar antes de implementar una decisión de negocio ambigua**, no adivinar.

**§19 — Construcción incremental:** cada etapa compila y no rompe la anterior.

**§23 — Formato de respuesta por fase:** Objetivo → Impacto → Archivos →
Decisiones técnicas → Riesgos → implementar → Resumen → Cómo ejecutar → Cómo
validar manualmente → Próximos pasos.

---

## 2. Cómo verificar: esto es lo más importante del archivo

**Claude no puede compilar este proyecto por su cuenta.** Ni Maven Central, ni
Docker Hub, ni el registro de npm son alcanzables desde sus entornos (403 en
todos). El hueco se cierra con un script que corre en la máquina del
propietario:

```powershell
.\scripts\verify.ps1 -Target all     # backend + frontend
.\scripts\verify.ps1                 # solo backend (por omisión)
.\scripts\verify.ps1 -SkipTests       # solo compilar
```

Deja dos archivos en la raíz: `build-report.txt` (lo esencial) y
`build-full.log` (todo). El propietario dice «listo» en el chat y Claude los lee
por el puente con su computadora.

### Leer el log, no solo el reporte

**Esta es la lección más costosa de todo el proyecto.** Cuatro veces hubo verde
que no significaba nada:

| Qué pasó | Cómo se veía | Por qué |
|---|---|---|
| Pruebas contra clases viejas | Todo en verde | Faltaba `clean` en el comando de Maven |
| Escrituras que no escribían | «Éxito» | `device_commit_files` reportaba bien y el archivo no cambiaba |
| ArchUnit evaluando cero clases | 8 reglas pasando | Su ASM no leía bytecode de Java 25, y `allowEmptyShould(true)` las hacía pasar vacías |
| Reporte sin evidencia del frontend | Reporte corto y limpio | Los patrones del filtro no casaban con la salida |

De ahí dos costumbres que **no hay que abandonar**:

1. **Después de cada corrida, revisar `build-full.log`,** no solo el reporte.
   Contar las pruebas por clase anidada y confirmar que las nuevas de verdad
   corrieron. El total del reporte puede cuadrar con pruebas que pasan vacías.
2. **Predecir el número de pruebas antes de correr.** Si el total no cuadra con
   lo que se agregó, algo se movió que no debía. Funcionó cada vez.

Existe `ArchitectureImportTest`, que afirma que ArchUnit importó más de 80
clases. Está ahí para que el tercer caso de la tabla no pueda repetirse en
silencio. **No lo borres.**

### Lo que Claude puede correr solo

En la VM Linux del puente (`device_bash`) sí hay Node y el `node_modules` del
proyecto, así que **antes de pedir el script** conviene correr:

```bash
npx tsc --noEmit    # tipos
npx eslint .        # lint
npx prettier --check "src/**/*.{ts,tsx,css,json}"
```

Lo que **no** se puede correr ahí: `vite build` y `vitest`. El `node_modules`
tiene los binarios nativos de rolldown para Windows y falla con
`Cannot find native binding`. Eso lo cubre el script, que corre en un contenedor
Linux con su propio `node_modules`.

Del backend no se puede correr nada: la VM tiene Java 11 y no tiene Maven.

---

## 3. Decisiones de arquitectura que sostienen el producto

Si alguna de estas se deshace, algo se rompe de forma no obvia.

### Toda la lógica financiera vive en el backend

El frontend presenta cifras ya calculadas y **nunca las deriva**. Hay una regla
de eslint (`no-restricted-syntax`) que avisa si aparece un `.toFixed()` sobre un
importe. Se puso en la Fase 1 y no sirvió para nada hasta la Fase 7; desde
entonces atrapó **cinco** violaciones reales, todas en camino al MVP:

- Un promedio de historial calculado en el cliente → se movió a
  `ItemHistoryEntry.averageActual`.
- La diferencia entre dos ciclos restada en el navegador → el servidor manda
  `outflowChange` con la dirección (`UP`/`DOWN`/`SAME`) aparte de la magnitud,
  que viaja siempre positiva.
- Un porcentaje derivado en un tooltip → se muestra el monto.
- `.replace('-', '')` para quitarle el signo a un balance negativo → se muestra
  con su signo.

**Corolario de diseño de API:** cuando la interfaz necesita una magnitud sin
signo o una comparación, el servidor la manda ya resuelta. No hay campos con
valor absoluto «por si acaso»: se agregan cuando una pantalla los necesita.

### El dinero es `BigDecimal` y `DECIMAL(15,2)`, nunca `double`

Hay una regla de ArchUnit que falla si aparece un campo `double`, `float`,
`Double` o `Float` en cualquier paquete del proyecto.

Los importes viajan por la API **como cadena**, no como número
(`MoneyDto(String amount, String currency)`), porque el tipo numérico de
JavaScript pierde precisión en importes grandes. La única excepción es
`progress` de una meta de ahorro y `savingsRate`/`expenseRate`, que son
proporciones de cuatro decimales y ahí no hay pérdida.

### Los renglones del ciclo son copias, no referencias

Un `CycleItem` guarda el nombre y el monto que la plantilla tenía cuando se
materializó el ciclo. Renombrar un gasto hoy **no** reescribe la historia.

De ahí la regla que se repite en cuatro módulos: **lo nuevo sí entra al ciclo
abierto, lo editado no.** Capturar un ingreso a media quincena y ver un
presupuesto sin él es desconcertante; que lo que revisaste ayer sea otra cosa
hoy es peor.

Lo implementa `CycleSyncListener` con eventos de Spring
(`ApplicationEventPublisher` + `@EventListener`), y eso **no es un detalle de
estilo**: mantiene la dependencia en un solo sentido. Presupuesto conoce a
ingresos, gastos y ahorros; ellos no saben que existe el presupuesto.

### La calculadora es código puro

`BudgetCalculator`, `DeficitAdvisor`, `SavingsPlanCalculator`, `CyclePlanner`,
`Proration`, `RecurrenceSchedule`: sin Spring, sin JPA, reciben un snapshot
inmutable (`PlannedItem`, `CutCandidate`) y se prueban en milisegundos. Las
entidades nunca entran ahí.

### Promesas del producto que están codificadas

**Un gasto `CRITICAL` nunca se propone recortar.** La pantalla de gastos lo dice
literalmente al capturarlo. `DeficitAdvisorTest` tiene una prueba que falla si
alguien lo cambia. No es una heurística ajustable.

**Confirmar el monto no es marcar un pago.** Son dos hechos distintos con dos
endpoints. `confirm-amounts` fija cuánto es y deja el renglón `PENDING`;
`settle` registra que ocurrió y lo deja `PAID`/`PARTIAL`. Juntarlos haría que el
balance diera por pagado lo que nadie pagó.

**Confirmar el aporte de ahorro es idempotente.** El movimiento guarda de qué
renglón del ciclo salió (`savings_contributions.cycle_item_id`), y un segundo
`settle` no vuelve a sumar. Es la defensa contra el doble conteo, que es el
riesgo central de ese módulo.

**Un ciclo cerrado es inmutable.** Ningún renglón se puede tocar después.

### El alta guiada no guarda borrador

Capturar un ingreso en el asistente **crea el ingreso**, con el mismo endpoint
que usa la sección de Ingresos. Cero tablas nuevas, cero migración, y volver
después no necesita recordar nada.

El riesgo —cuentas abandonadas con datos sueltos— lo atiende
`AbandonedOnboardingJob`, y **funciona sin una columna nueva por una razón que
conviene entender antes de tocarlo**: el asistente es obligatorio, así que una
cuenta con `onboarding_completed_at` nulo no tiene otra forma de haber creado
nada. La ausencia de esa fecha *es* la marca.

Tres reglas de ese trabajo:
- **Invalida, no borra** (borrado lógico, auditable).
- **Se mide desde la última captura**, no desde el registro.
- **Ante la duda no toca nada.** Si `created_at` llega nulo —lo pone MySQL con
  su `DEFAULT` y la columna está mapeada como no insertable— deja la cuenta en
  paz. Equivocarse invalidando datos de alguien que sigue trabajando es mucho
  peor que dejar una cuenta sin limpiar un día más.

### La paleta de las gráficas está verificada, no elegida a ojo

`theme/tokens.ts` → `chartPalette`. Los valores salieron de correr el validador
de la guía de visualización (`dataviz`) y son los que pasaron las cinco
comprobaciones. **Dos cosas que no hay que deshacer:**

- **El modo oscuro no es el claro aclarado.** `honey` y `positive` del tema
  oscuro quedan sobre el techo de luminosidad para un relleno (L 0.82 y 0.74
  contra un máximo de 0.67): como texto funcionan, como área grande brillan. De
  ahí los pasos propios `#C3862A` / `#2A7A50`.
- **«Disponible» no es un color, es el hueco.** Como cuarto segmento gris
  fallaba dos comprobaciones: un gris no alcanza el piso de croma, y contra el
  verde del ahorro daba ΔE 2.4 en visión deutan — indistinguibles.

En la gráfica de balances, **el color no dice el signo**: lo dice la posición
respecto al cero y la etiqueta de cada barra. Verde/rojo falla daltonismo con
ΔE 3.3 en protan, así que es refuerzo convencional y nada más. Un escenario de
automatización que verifique el color de una barra estaría verificando la
decoración.

### Reordenar renglones del ciclo: se decidió NO hacerlo

Está en la especificación desde la Fase 0, pero los renglones se ordenan por
fecha de vencimiento, que es el único orden que carga información en una lista
de pagos. Un orden manual pelearía contra eso. El campo `displayOrder` se quedó
como desempate en la materialización.

### Decisiones de negocio tomadas sin preguntar (y fáciles de revertir)

- **El orden del recorte ante déficit:** gastos flexibles → ahorros (del menos
  prioritario al más) → gastos importantes. Lo discutible es si el ahorro
  debería ir primero: no apartar no le cuesta nada a nadie hoy. Son tres líneas
  en `DeficitAdvisor`.
- **En el paso de ahorro del asistente solo se ofrece aporte fijo por ciclo**,
  no los tres modos. En el alta la persona no tiene con qué elegir entre tres
  formas de calcular.
- **El bloque de ahorros vive en la pantalla de revisión** aunque un renglón de
  ahorro no esté en `NEEDS_REVIEW`. Sin él, `registerInGoal` no tenía interfaz
  en ninguna parte.

---

## 4. Estado por módulo

| Módulo | Backend | Pantalla | Notas |
|---|---|---|---|
| Autenticación | ✅ | ✅ | Sesión persistente con rotación de refresh token; recuperación por correo |
| Preferencias | ✅ | ✅ | `/users/me/preferences`; el tipo de ciclo se cambia desde Ajustes |
| Motor presupuestal | ✅ | — | Dominio puro; quincenal, mensual y bimestral |
| Ciclos | ✅ | ✅ | Abrir y cerrar desde el resumen; cerrado = inmutable |
| Ingresos | ✅ | ✅ | |
| Gastos | ✅ | ✅ | Fijos y variables en un solo recurso; 17 categorías sembradas en V2 |
| Ahorros | ✅ | ✅ | Arrastrar y soltar nativo + botones subir/bajar (táctil y teclado) |
| Revisión por ciclo | ✅ | ✅ | «Este ciclo»; sugerencia del ciclo pasado, historial, lote todo-o-nada |
| Alta guiada | ✅ | ✅ | Cinco pasos; solo ingresos es obligatorio |
| Resumen financiero | ✅ | ✅ | Balance, consejo de déficit, comparación con gráfica |

**Migraciones:** V1 (usuarios y auth) y V2 (núcleo presupuestal). `ddl-auto`
nunca pasa de `validate`. **No hizo falta una V3 en ninguna fase** — vale la
pena intentar mantener esa racha: dos de las últimas cuatro fases parecían
necesitarla y no la necesitaban.

---

## 5. Deuda pendiente, con su razón

| Qué | Por qué quedó pendiente |
|---|---|
| **ESLint 10** | El 9.x quedó sin soporte (`9.39.5` avisa en cada build). Es un salto mayor: config plana, compatibilidad de `typescript-eslint` y plugins de React. **No se puede verificar desde aquí** — el registro de npm responde 403 en los dos entornos de Claude. Es cambiar la herramienta que avisa cuando algo está mal, a ciegas. |
| **SHA de Gitleaks** | `security.yml` lo tiene fijado a `@v2.3.9` en lugar del SHA completo. La API de GitHub dio 403 desde los tres caminos disponibles. El archivo tiene el enlace y el paso exacto para cerrarlo en un minuto. Trivy sí quedó por SHA (`ed142fd…`, v0.36.0). |
| **`@mui/x-charts` sin fijar** | Está como `^9.14.0`. |
| **Etiquetas de la gráfica en teléfono** | Seis barras con su monto encima, y no hay forma de renderizar desde aquí para ver si se encaman a ancho de teléfono. La etiqueta es la codificación secundaria del signo, así que no se puede quitar sin más: si colisionan, la salida es ocultarlas solo en pantallas angostas y dejar el tooltip. **Es lo único del MVP sin verificar.** |
| **Política `PRORATE`** | Elegirla responde 422 con un mensaje que pide cambiarla a `BY_DUE_DATE`. Deliberado: el motor falla en voz alta antes que calcular con una regla distinta de la que la persona eligió. Los escenarios están en `99-escenarios-por-fase.md`, sin fase asignada. |
| **Gitleaks y organizaciones** | `gitleaks-action` exige `GITLEAKS_LICENSE` en cuentas de organización. En un repositorio personal es gratis. El día que el proyecto se mude, ese paso empieza a fallar pidiendo licencia. |

---

## 6. Siguientes pasos

### La etapa que el propietario hace él: automatización de pruebas

Es lo que sigue de forma natural, y **Claude no participa en construirla** (§3).
Lo que sí hay listo como materia prima:

- **12 documentos de escenarios** en Gherkin (`# language: es`), en
  `docs/testing/`. Cada bloque ` ```gherkin ` es un `.feature` válido tal cual.
- Etiquetas: `@listo` (automatizable hoy), `@pendiente` (no existe aún), `@api`,
  `@ui`, `@smoke`, `@critico`, `@seguridad`, `@responsive`, `@fase-N`,
  `@requiere-semilla`.
- **`docs/testids.md`**: el catálogo completo de `data-testid` tratado como API
  pública de la interfaz. No se renombran sin anotarlo ahí.

Tres advertencias que ya están escritas en `testids.md` y que ahorran
escenarios frágiles:

1. **Las pantallas se cargan al visitarlas.** Cada ruta es un trozo aparte
   (`React.lazy`), así que entre el clic y el contenido aparece `state-loading`
   un instante, incluso con datos en caché. Esperar al elemento, no al clic.
2. **Varios testids solo existen condicionalmente:** `dashboard-advice` solo con
   déficit, `savings-move-up` deshabilitado en el primero,
   `review-row-use-suggestion` solo si el campo no tiene ya la sugerencia,
   la insignia del menú solo si hay pendientes.
3. **No afirmar sobre el color de una barra.**

### Fases posteriores al MVP

Los escenarios pendientes están en `docs/testing/99-escenarios-por-fase.md`.

- **Fase 11 — Alertas.** Pago próximo, pago vencido, déficit, contador de no
  leídas. Ya existen `OverdueItemsJob` y `RefreshTokenCleanupJob` como
  precedente de trabajo programado.
- **Fase 12 — Planes y suscripciones.** Plan gratuito, funciones premium,
  comunicar el límite con claridad.
- **Fase 13 — Análisis financiero.** Aquí entra la IA, y con una propiedad que
  **no es negociable**: la IA nunca calcula cifras, solo interpreta las que ya
  calculó el motor. `BudgetResult` se diseñó desde la Fase 4 para ser
  justamente lo que ese módulo consume. Cada recomendación muestra las cifras
  que la respaldan, y nunca sugiere retrasar un pago crítico.

### Mejoras que valdría la pena, sin fase asignada

- Cerrar la deuda de la tabla de arriba, empezando por ESLint (necesita que el
  propietario corra el `npm install`).
- La tarjeta de Ajustes solo permite cambiar el tipo de ciclo y el día de
  anclaje. Moneda, idioma y zona horaria tienen endpoint de lectura pero no de
  escritura.
- El bundle: la entrada está en 428 kB (137 kB gzip) y ningún trozo pasa el
  umbral de Vite. Si vuelve a crecer, el siguiente corte natural es separar
  `@mui/material` del trozo de entrada.

---

## 7. Cómo trabajar en este proyecto

Lo que funcionó durante diez fases, por si sirve de guía:

1. **Preguntar las decisiones de producto antes de escribir** (§24). Las fases
   grandes empezaron con tres o cuatro preguntas de opción múltiple sobre cosas
   que cambiaban la arquitectura, no el detalle.
2. **Fase completa: backend + pantalla + documentos + escenarios**, y después
   una sola corrida de verificación.
3. **Predecir el número de pruebas** antes de correr el script.
4. **Leer el log completo**, no solo el reporte.
5. **Cuando el lint o una comprobación se queja, casi siempre tiene razón.** Las
   cinco violaciones de aritmética de dinero y los dos problemas de paleta
   salieron de ahí, no de una revisión a ojo.
6. **Trabajar directamente en la carpeta del propietario** con `device_bash`.
   Editar en su máquina y no en el contenedor de la nube evitó toda una clase de
   errores: hubo tres escrituras que reportaron éxito sin escribir cuando se
   usaba el otro camino.

### Documentos de referencia

| Archivo | Qué contiene |
|---|---|
| `README.md` | Estado por fase, arquitectura en tres decisiones, cómo levantar |
| `docs/00-arquitectura-fase-0.md` | El análisis y el plan completo |
| `docs/api.md` | Convenciones de la API y el razonamiento de cada decisión de contrato |
| `docs/architecture.md` | Módulos y fronteras |
| `docs/design-system.md` | Tokens, tema, lenguaje |
| `docs/development.md` | Requisitos, comandos, convenciones de código |
| `docs/testids.md` | El catálogo de selectores, como contrato |
| `docs/testing/` | Los 12 documentos de escenarios + el índice + lo pendiente |
