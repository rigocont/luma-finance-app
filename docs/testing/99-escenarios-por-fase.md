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

---

## Fases 11 a 13

```gherkin
# language: es

@pendiente @fase-11 @notificaciones
Característica: Alertas

  Escenario: Un pago próximo genera una alerta
  Escenario: Un pago vencido genera una alerta
  Escenario: Un déficit genera una alerta
  Escenario: Marcar una alerta como leída reduce el contador

@pendiente @fase-12 @suscripciones
Característica: Planes

  Escenario: Una cuenta nueva empieza en el plan gratuito
  Escenario: Una función premium no está disponible en el plan gratuito
  Escenario: El límite del plan gratuito se comunica con claridad

@pendiente @fase-13 @analisis
Característica: Análisis financiero

  Escenario: Se detecta un déficit y se explica qué lo causó
  Escenario: Se detecta un remanente y se sugiere repartirlo entre metas
  Escenario: Se detecta un crecimiento sostenido en una categoría
  Escenario: Nunca se sugiere retrasar un pago marcado como crítico
  Escenario: Cada recomendación muestra las cifras que la respaldan
```

El último escenario es una propiedad de seguridad del producto, no un detalle:
la IA nunca calcula cifras, solo interpreta las que ya calculó el motor.

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
