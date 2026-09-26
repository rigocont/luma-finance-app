# Escenarios por construir

Lo que llega en fases posteriores. **Nada de esto es automatizable todavía:**
la funcionalidad no existe.

Sirve para dos cosas: ver hacia dónde va la cobertura, y que cada fase se
construya sabiendo qué comportamiento tendrá que demostrar.

Cada fase, al completarse, mueve sus escenarios a un documento propio con el
mismo formato que los anteriores.

**Ya salieron de aquí:**

| Fase | Se fue a |
|---|---|
| 2 — Sesión persistente y recuperación | [`01-autenticacion.md`](01-autenticacion.md), [`04-api-autenticacion.md`](04-api-autenticacion.md), [`05-recuperacion-contrasena.md`](05-recuperacion-contrasena.md) |
| 4 — Motor presupuestal y ciclos | [`06-ciclos-presupuestales.md`](06-ciclos-presupuestales.md) |
| 5 — Ingresos | [`07-ingresos.md`](07-ingresos.md) |
| 6 — Gastos fijos y variables | [`08-gastos.md`](08-gastos.md) |
| 8 — Ahorros | [`09-ahorros.md`](09-ahorros.md) |
| 7 — Revisión por ciclo | [`10-revision-del-ciclo.md`](10-revision-del-ciclo.md) |
| 9 — Onboarding | [`11-alta-guiada.md`](11-alta-guiada.md) |
| 10 — Resumen financiero | [`12-resumen-financiero.md`](12-resumen-financiero.md) |
| 11 — Alertas | [`13-alertas.md`](13-alertas.md) |
| 12 — Anuncios | [`14-anuncios.md`](14-anuncios.md) |
| 13 — Analisis financiero (parte determinista) | [`15-analisis-financiero.md`](15-analisis-financiero.md) |

---

## Fase 14

```gherkin
# language: es

@pendiente @fase-14 @tour
Característica: Tour guiado de bienvenida

  Escenario: Una cuenta nueva ve el tour al entrar por primera vez
  Escenario: El tour se puede omitir en cualquier paso
  Escenario: Omitir el tour no lo vuelve a mostrar solo en la siguiente sesión
  Escenario: Desde Ajustes se puede volver a tomar el tour completo
  Escenario: El tour explica cada pantalla principal, un paso a la vez
```

Es estético y de aprendizaje, no de negocio: no calcula ni guarda nada del
presupuesto. Vive casi enteramente en el frontend, como una preferencia más de
la cuenta -si ya lo vio, se guarda para no repetírselo solo la siguiente vez
que inicie sesión-.

---

## Fase 15

```gherkin
# language: es

@pendiente @fase-15 @idioma
Característica: Interfaz bilingüe (español / inglés)

  Escenario: Una cuenta nueva ve la interfaz en el idioma de su sistema, si es uno de los dos soportados
  Escenario: Una cuenta nueva en un idioma no soportado ve la interfaz en español, por defecto
  Escenario: Cambiar el idioma desde Ajustes traduce toda la interfaz al instante
  Escenario: El idioma elegido se recuerda entre sesiones, incluso en otro dispositivo
  Escenario: Lo que la persona escribió -nombres de gastos, metas, notas- nunca se traduce
  Escenario: Los montos y las fechas se formatean según el idioma, no solo se traducen las palabras
```

Es de presentación, no de dominio: ningún cálculo cambia entre idiomas. Del
servidor viaja sin traducir todo lo que la persona capturó; toda oración fija
de la interfaz (títulos, botones, mensajes, y los mapas como
`STATE_HEADLINE` en el resumen financiero) sale de un catálogo de
traducciones por idioma, no de la cadena en español que hoy está escrita a
mano. Es la misma idea que ya usa el proyecto para traducir códigos estables
del servidor -`BudgetState`, tipos de alerta- llevada a dos idiomas en vez de
a una sola redacción.

---

## Sin fase asignada

```gherkin
# language: es

@pendiente @presupuesto
Característica: Política de prorrateo de gastos

  Escenario: Un gasto mensual se reparte entre las quincenas del mes
  Escenario: Los centavos de un reparto inexacto se asignan sin perder ni un centavo
  Escenario: Cambiar de prorrateo a fecha de vencimiento aplica desde el siguiente ciclo
```

Hoy elegir `PRORATE` devuelve 422 con un mensaje claro. Es deliberado: el motor
falla en voz alta antes que calcular con una regla distinta de la que la persona
eligió.
