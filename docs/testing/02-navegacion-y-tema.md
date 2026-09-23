# Navegación, tema y estructura

Navegación SPA, comportamiento responsive, tema claro/oscuro y rutas inexistentes.

**Estado:** implementado en las Fases 1 y 1.5.

---

## Navegación

```gherkin
# language: es

@ui @navegacion
Característica: Moverse entre secciones
  Como usuario con sesión iniciada
  Quiero navegar por la aplicación
  Para llegar a lo que necesito sin recargar la página

  Antecedentes:
    Dado que inicié sesión
    Y que estoy en el resumen

  @listo @smoke
  Esquema del escenario: Ir a cada sección desde la barra lateral
    Cuando selecciono "<seccion>" en la barra lateral
    Entonces el título de la página es "<titulo>"
    Y la dirección del navegador cambia
    Y la página no se recarga por completo

    Ejemplos:
      | seccion          | titulo           |
      | Ingresos         | Ingresos         |
      | Gastos fijos     | Gastos fijos     |
      | Gastos variables | Gastos variables |
      | Ahorros          | Ahorros          |
      | Ajustes          | Ajustes          |
      | Resumen          | Hola             |

  @listo
  Escenario: La sección activa se distingue en la barra lateral
    Cuando selecciono "Ahorros" en la barra lateral
    Entonces "Ahorros" aparece marcado como la sección actual
    Y ninguna otra sección aparece marcada

  @listo
  Escenario: Las secciones aún no construidas lo dicen
    Cuando selecciono "Ingresos" en la barra lateral
    Entonces la página indica que la sección llega pronto
    Y menciona en qué fase se construye
    # Se muestran en lugar de ocultarse a propósito: la navegación completa
    # deja ver hacia dónde va el producto.

  @listo
  Escenario: Volver con el botón de atrás del navegador
    Dado que fui a la sección de ahorros
    Cuando uso el botón de atrás del navegador
    Entonces regreso al resumen

  @listo
  Escenario: Una dirección que no existe
    Cuando abro una dirección que no corresponde a ninguna sección
    Entonces la página indica que no se encontró
    Y ofrece volver al resumen
    Y al usar esa opción llego al resumen
```

---

## Comportamiento responsive

```gherkin
# language: es

@ui @navegacion @responsive
Característica: La aplicación se adapta al ancho de la pantalla
  Como usuario que entra desde el teléfono
  Quiero que la aplicación siga siendo usable
  Para no tener que abrir la computadora

  Antecedentes:
    Dado que inicié sesión

  @listo
  Escenario: En escritorio la barra lateral está siempre visible
    Dado que la ventana tiene un ancho de escritorio
    Entonces la barra lateral está visible
    Y no hay botón de menú

  @listo @critico
  Escenario: En móvil la navegación se abre desde el menú
    Dado que la ventana tiene un ancho de teléfono
    Entonces la barra lateral no está visible
    Y hay un botón de menú
    Cuando abro el menú
    Entonces la barra lateral aparece

  @listo
  Escenario: Al elegir una sección en móvil el menú se cierra
    Dado que la ventana tiene un ancho de teléfono
    Y que abrí el menú
    Cuando selecciono "Ahorros"
    Entonces llego a la sección de ahorros
    Y el menú se cierra solo

  @listo
  Esquema del escenario: La página nunca se desborda a lo ancho
    Dado que la ventana tiene un ancho de <ancho> píxeles
    Cuando abro el resumen
    Entonces la página no tiene desplazamiento horizontal

    Ejemplos:
      | ancho |
      | 400   |
      | 768   |
      | 1280  |
      | 1920  |
```

---

## Tema claro y oscuro

```gherkin
# language: es

@ui @tema
Característica: Elegir entre tema claro y oscuro
  Como usuario
  Quiero elegir cómo se ve la aplicación
  Para que me resulte cómoda a cualquier hora

  @listo
  Escenario: Cambiar el tema desde el encabezado
    Dado que inicié sesión
    Y que la aplicación está en tema claro
    Cuando cambio el tema
    Entonces la aplicación se muestra en tema oscuro
    Y el control ofrece volver al tema claro

  @listo
  Escenario: La preferencia de tema sobrevive a una recarga
    Dado que inicié sesión
    Y que cambié al tema oscuro
    Cuando recargo la página
    Entonces la aplicación sigue en tema oscuro
    # Contraste deliberado con la sesión: el tema SÍ se persiste (clave
    # "luma-ui" en el almacenamiento local) porque no es información sensible.
    # El token no se persiste. Ver 01-autenticacion.md.

  @listo
  Escenario: Las pantallas de sesión también respetan el tema
    Dado que la aplicación está en tema oscuro
    Cuando abro la pantalla de inicio de sesión
    Entonces se muestra en tema oscuro

  @listo
  Escenario: El tema por defecto sigue al del sistema
    Dado que nunca he elegido un tema
    Y que el sistema operativo está en modo oscuro
    Cuando abro la aplicación
    Entonces se muestra en tema oscuro
```

---

## Mapa con el catálogo de selectores

| Concepto en los escenarios | `data-testid` |
|---|---|
| Contenedor principal | `layout-app-shell` |
| Barra lateral | `layout-sidebar` |
| Botón de menú (móvil) | `layout-sidebar-toggle` |
| Encabezado | `layout-header` |
| Cambio de tema | `layout-theme-toggle` |
| Elemento de navegación | `layout-nav-dashboard`, `layout-nav-incomes`, `layout-nav-fixed-expenses`, `layout-nav-variable-expenses`, `layout-nav-savings`, `layout-nav-settings` |
| Título de la página | `layout-page-title` |
| Estado vacío | `state-empty` |
