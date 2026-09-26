# Interfaz bilingue (espanol / ingles)

Toda la aplicacion -no solo un catalogo de infraestructura- traducida a dos
idiomas: espanol (por defecto) e ingles. Lo que la persona escribe -nombres
de gastos, metas, notas- nunca pasa por el traductor.

**Estado:** Fase 15. Vive enteramente en el frontend con `i18next` y
`react-i18next`. El backend no sabe que existen dos idiomas: sigue
respondiendo codigos estables (`BudgetState`, tipos de alerta, tipos de
gasto...) que el frontend ya traducia antes de esta fase, ahora a dos idiomas
en vez de a una sola redaccion en espanol.

---

## Como esta armado

Un solo namespace por defecto y dos catalogos planos
(`src/i18n/locales/es.json` y `en.json`), en vez de uno por seccion: con este
tamano de aplicacion, partirlo en dieciseis archivos costaria mas
coordinacion de la que ahorra.

**Deteccion inicial.** Al arrancar sin sesion, `detectSystemLanguage()` mira
`navigator.language`: si empieza con `en` usa ingles, cualquier otro caso cae
en espanol. Es una sola comparacion, por eso no se trajo
`i18next-browser-languagedetector` como dependencia.

**`uiLanguage` es de la cuenta, no del navegador.** En cuanto hay sesion,
`useLanguageSync()` -montado una sola vez dentro de `AppShell`, que solo
renderiza para sesiones autenticadas y ya dadas de alta- sobreescribe el
idioma detectado con lo que la cuenta tenga guardado en
`preferences.uiLanguage`. Cambiar el idioma desde Ajustes llama a
`useUpdateLanguage()`, que aplica el cambio de inmediato en la interfaz
(`onMutate` llama a `i18n.changeLanguage(...)` antes de que responda el
servidor) y lo persiste en la cuenta, asi que **el idioma elegido se recuerda
entre sesiones, incluso en otro dispositivo**: no es una preferencia de
navegador como el tema.

**`uiLanguage` (palabras) y `locale` (numeros y fechas) son cosas
distintas.** `intlLocaleFor(uiLanguage)` traduce `'en'` a `'en-US'` y
cualquier otro caso a `'es-MX'`; `formatMoney`, `formatMoneyCompact` y el
formateador de fechas en `lib/date` lo recalculan en cada llamada, nunca lo
cachean en una constante de modulo. Que la interfaz se lea en ingles no
implica que "$1,100.00" deba escribirse "1.100,00 $": son dos decisiones
independientes que hoy coinciden porque solo hay dos idiomas.

**Datos vs. presentacion.** Todo mapa de la forma `ENUM -> texto` (tipo de
ingreso, flexibilidad de un gasto, estado de una meta, tipo de movimiento,
paso del asistente...) se convirtio de una constante `Record<Enum, string>` a
una funcion `algoLabel(value)` que llama a `i18n.t(...)`. Un archivo que no es
componente -un `types.ts`, un cliente de API, un hook- no puede llamar a
`useTranslation()`, asi que usa `import i18n from '@/i18n'; i18n.t(key)`
directamente. Sigue siendo reactivo a un cambio de idioma porque el
COMPONENTE que lo llama ya usa `useTranslation()` para su propio texto: al
cambiar el idioma, react-i18next fuerza su render y esa funcion se vuelve a
invocar con el idioma nuevo.

**Lo que la persona escribio nunca se traduce.** El nombre de un gasto, una
meta, una nota: viaja intacto del servidor a la pantalla, en cualquier
idioma. No hay una sola linea en este cambio que envuelva `income.name`,
`expense.notes` o similares en `t(...)`.

**Mensajes de error del backend: limitacion conocida y documentada.** Los
mensajes que genera el servidor (`problem.detail` de una
`IllegalArgumentException`, una `BusinessRuleException`, o de una validacion
de Bean Validation) siguen en espanol sin importar el idioma de la interfaz.
Traducirlos requeriria que el backend negocie idioma por cabecera y mantenga
su propio catalogo, un cambio de otro tamano que queda fuera de esta fase. Lo
que si se tradujo son los mensajes que el FRONTEND redacta el mismo: los
cuatro mensajes genericos por codigo de estado en `lib/api/client.ts` (sin
conexion, sesion expirada, sin acceso, no encontrado, error del servidor).

---

## La interfaz

```gherkin
# language: es

@ui @idioma
Caracteristica: Interfaz bilingue (espanol / ingles)
  Como persona que usa LUMA en espanol o en ingles
  Quiero que toda la aplicacion se lea en mi idioma
  Para no depender de traducir yo misma cada pantalla

  @listo @smoke
  Escenario: Una cuenta nueva ve la interfaz en el idioma de su sistema, si es uno de los dos soportados
    Dado un navegador configurado en ingles y sin sesion iniciada
    Cuando entra a la aplicacion
    Entonces toda la interfaz aparece en ingles

  @listo
  Escenario: Una cuenta nueva en un idioma no soportado ve la interfaz en espanol, por defecto
    Dado un navegador configurado en frances y sin sesion iniciada
    Cuando entra a la aplicacion
    Entonces la interfaz aparece en espanol

  @listo @critico
  Escenario: Cambiar el idioma desde Ajustes traduce toda la interfaz al instante
    Dado una cuenta con sesion iniciada, viendo la interfaz en espanol
    Cuando elige "English" en la tarjeta de idioma de Ajustes
    Entonces toda la interfaz visible cambia a ingles sin recargar la pagina

  @listo @critico
  Escenario: El idioma elegido se recuerda entre sesiones, incluso en otro dispositivo
    Dado una cuenta que eligio ingles desde Ajustes
    Cuando cierra sesion y vuelve a entrar, o entra desde otro navegador
    Entonces la interfaz aparece en ingles sin que la persona lo vuelva a elegir

  @listo
  Escenario: Lo que la persona escribio nunca se traduce
    Dado un gasto llamado "Renta del departamento" capturado en espanol
    Cuando la cuenta cambia el idioma de la interfaz a ingles
    Entonces el gasto sigue llamandose "Renta del departamento"

  @listo
  Escenario: Los montos y las fechas se formatean segun el idioma, no solo se traducen las palabras
    Dado un balance de 1100 pesos con la interfaz en ingles
    Cuando se muestra el monto
    Entonces aparece como "$1,100.00" y no como "1.100,00 $"

  @listo
  Escenario: Un mensaje de error generado por el propio frontend se traduce
    Dado la interfaz en ingles y la sesion sin conexion al servidor
    Cuando una peticion falla por falta de red
    Entonces el aviso aparece en ingles

  @conocido
  Escenario: Un mensaje de error generado por el backend permanece en espanol
    Dado la interfaz en ingles
    Cuando el servidor rechaza una peticion por una regla de negocio
    Entonces el texto del error llega en espanol, tal como lo redacto el backend
```

`@conocido` marca una limitacion aceptada, no un pendiente: el escenario
existe para que quede escrito por que ese mensaje concreto no se traduce, y
no se confunda con un olvido en una revision futura.

---

## Cobertura automatizada hoy

| Prueba | Que cubre |
|---|---|
| `setupTests.ts` fuerza `i18n.changeLanguage('es')` | Las ~100 pruebas de componente existentes, escritas contra texto en espanol, no dependen de `navigator.language` del entorno de pruebas |
| Pruebas de componente existentes (dashboard, ingresos, gastos, ahorros, revision, alta guiada, ajustes...) | Cada pantalla sigue renderizando el texto esperado ahora que ese texto sale de `i18n.t(...)` en vez de estar escrito a mano |

Los escenarios de deteccion de idioma, cambio desde Ajustes y persistencia
entre sesiones (los primeros cuatro de arriba) son de extremo a extremo y
hoy se verifican a mano: exigen variar `navigator.language`, tener dos
sesiones o dos navegadores, algo que las pruebas de componente actuales no
levantan. Quedan como candidatos naturales para la siguiente pasada de
pruebas end-to-end del proyecto, no como deuda de esta fase.
