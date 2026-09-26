# Tour guiado de bienvenida

Un recorrido corto que explica las pantallas principales a una cuenta nueva,
sin tocar ningun dato de negocio: no calcula ni guarda nada del presupuesto.

**Estado:** Fase 14. Vive casi enteramente en el frontend, como una
preferencia de presentacion mas -no hay endpoint que la sostenga, y no
deberia haberlo: si ya se vio o se omitio, no vale la pena una peticion al
servidor para saberlo-.

---

## Como esta armado

El tour no navega entre rutas. Cada paso senala el elemento de navegacion de
una pantalla en la barra lateral (`layout-nav-<key>`, siempre montada dentro
de `AppShell`) en vez de llevar a la persona a esa pantalla: cambiar de ruta
a mitad del recorrido le haria perder el trabajo si ya estaba usando una de
ellas. Los pasos de apertura y cierre son centrados, sin objetivo.

`GuidedTour` esta montado una sola vez dentro de `AppShell` -junto a
`AdSlot`-, asi que "Volver a tomar el tour" desde Ajustes lo puede arrancar
sin importar en que pantalla este la persona: ambos leen el mismo store
(`tourStore`).

Se guarda solo `completed` (si ya se vio, completo u omitido). El paso
activo y si esta corriendo son de la sesion en curso y no se persisten: si se
recarga la pagina a mitad de un paso, el tour no reaparece congelado ahi.

---

## La interfaz

```gherkin
# language: es

@ui @tour
Caracteristica: Tour guiado de bienvenida
  Como cuenta nueva en LUMA
  Quiero un recorrido corto por las pantallas principales
  Para saber donde esta cada cosa antes de usarla

  @listo @smoke
  Escenario: Una cuenta nueva ve el tour al entrar por primera vez
    Dado una cuenta que nunca vio ni omitio el tour en este navegador
    Cuando entra a la aplicacion
    Entonces aparece el primer paso del tour, centrado y sin objetivo

  @listo
  Escenario: El tour se puede omitir en cualquier paso
    Dado el tour corriendo en cualquier paso
    Cuando la persona pulsa "Omitir"
    Entonces el tour se cierra de inmediato

  @listo @critico
  Escenario: Omitir el tour no lo vuelve a mostrar solo en la siguiente sesion
    Dado que la persona omitio el tour
    Cuando recarga la aplicacion o vuelve a iniciar sesion
    Entonces el tour no aparece solo

  @listo @critico
  Escenario: Desde Ajustes se puede volver a tomar el tour completo
    Dado una cuenta que ya vio u omitio el tour antes
    Cuando pulsa "Volver a tomar el tour" en Ajustes
    Entonces el tour arranca de nuevo desde el primer paso

  @listo
  Escenario: El tour explica cada pantalla principal, un paso a la vez
    Dado el tour corriendo
    Cuando avanza con "Siguiente"
    Entonces cada paso intermedio senala el elemento de navegacion de una
      pantalla distinta -Resumen, Este ciclo, Ingresos, Gastos, Ahorros,
      Ajustes-, en ese orden
```

---

## Cobertura automatizada hoy

| Prueba | Que cubre |
|---|---|
| `tourStore.test.ts` | Transiciones del estado (`start`, `next`/`prev`, `skip`, `finish`) y que solo `completed` sobrevive una recarga |
| `GuidedTour.test.tsx` | Arranque automatico solo la primera vez, avance/retroceso de pasos, que omitir y terminar marquen completado y no reaparezca solo, y que "Ajustes" lo pueda volver a arrancar manualmente |

Los escenarios `@ui` de arriba describen el comportamiento visible; las
pruebas de componente y de store cubren lo mismo sin necesitar un navegador.
