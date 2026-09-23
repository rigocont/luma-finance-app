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

---

## Fases 6 a 8 — Gastos y ahorros

Los ingresos ya tienen CRUD ([`07-ingresos.md`](07-ingresos.md)). Faltan gastos,
categorías y metas: hasta que existan, los escenarios de
[`06-ciclos-presupuestales.md`](06-ciclos-presupuestales.md) marcados
`@requiere-semilla` siguen dependiendo de filas insertadas por SQL.

```gherkin
# language: es

@pendiente @fase-6 @gastos-fijos
Característica: Gestión de gastos fijos

  Escenario: Crear un gasto fijo con categoría y vencimiento
  Escenario: El catálogo de categorías del sistema está disponible
  Escenario: Crear una categoría propia
  Escenario: Una categoría propia no es visible para otro usuario
  Escenario: Marcar un gasto como crítico, importante o flexible
  Escenario: Desactivar un gasto fijo
  Escenario: Un gasto desactivado no aparece en el siguiente ciclo
  Escenario: Eliminar un gasto fijo pide confirmación
  Escenario: Borrar una categoría deja los gastos sin categoría, no los borra

@pendiente @fase-7 @gastos-variables
Característica: Revisión de gastos variables por ciclo

  Escenario: Confirmar varios gastos a la vez
  Escenario: El contador de pendientes de revisión baja al confirmar
  Escenario: El historial muestra el monto de cada ciclo anterior
  Escenario: La captura sugiere el monto del ciclo pasado

@pendiente @fase-8 @ahorros
Característica: Metas de ahorro

  Escenario: Crear una meta con monto objetivo y fecha
  Escenario: El aporte por ciclo se calcula solo a partir de la fecha objetivo
  Escenario: Crear una meta con aporte fijo por ciclo
  Escenario: Una meta en modo manual no entra en el presupuesto
  Escenario: Registrar una aportación actualiza el progreso
  Escenario: Registrar un retiro reduce el progreso
  Escenario: Una meta alcanzada se marca como completada
  Escenario: Reordenar las metas por prioridad arrastrando
  Escenario: La proyección dice cuándo se alcanzará la meta al ritmo actual
  Escenario: Una fecha objetivo en el pasado se rechaza
```

---

## Fase 9 — Onboarding

```gherkin
# language: es

@pendiente @fase-9 @onboarding
Característica: Asistente de configuración inicial

  Escenario: Una cuenta nueva entra directo al asistente
  Escenario: Avanzar y retroceder entre pasos conserva lo capturado
  Escenario: Salir a la mitad y volver retoma donde se quedó
  Escenario: No se puede avanzar sin completar lo obligatorio
  Escenario: Elegir gastos sugeridos del catálogo
  Escenario: Agregar un gasto que no está en el catálogo
  Escenario: Elegir el tipo de ciclo y el día de anclaje
  Escenario: El resumen final muestra el balance calculado
  Escenario: Al terminar se crea el primer ciclo y se llega al resumen
  Escenario: Una cuenta que ya completó el asistente no vuelve a verlo
```

---

## Fase 10 — Resumen financiero

Aquí llega la interfaz del presupuesto. Los escenarios `@api` del motor ya
existen en [`06-ciclos-presupuestales.md`](06-ciclos-presupuestales.md); estos
son los `@ui` que faltan.

```gherkin
# language: es

@pendiente @fase-10 @dashboard
Característica: Resumen financiero

  Escenario: Los totales coinciden con lo registrado
  Escenario: El estado del presupuesto se dice en lenguaje normal
  Escenario: Los próximos pagos se listan por fecha
  Escenario: Los pagos vencidos se destacan
  Escenario: Marcar un pago como realizado actualiza el balance
  Escenario: El déficit se muestra siempre acompañado de su explicación
  Escenario: El porcentaje destinado a ahorro se calcula correctamente
  Escenario: Las tendencias comparan los últimos ciclos
  Escenario: Un ciclo sin renglones muestra un estado vacío, no un error
  Escenario: Los renglones por revisar se señalan al entrar al ciclo
  Escenario: Reordenar renglones arrastrando conserva el orden al recargar
  Escenario: Abrir el siguiente ciclo desde la interfaz
  Escenario: Cerrar el ciclo pide confirmación y advierte que será inmutable
  Escenario: Un ciclo cerrado se muestra en modo lectura
```

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
