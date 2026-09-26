# Anuncios

Como LUMA se sostiene sin cobrar por el uso: un espacio de anuncio al final de
cada pantalla dentro de la aplicacion.

**Estado:** Frontend en la Fase 12. Sin API ni tabla propias -no hay nada que
el servidor calcule o guarde sobre esto-, asi que este documento no tiene
escenarios @api.

---

## La decision

LUMA no cobra por usarse. No hay plan FREE ni PREMIUM, ni una funcion que se
desbloquee pagando: la version publicada es la unica version que existe. La
monetizacion es por anuncios, y el espacio vive en `AppShell`, no dentro de
cada pantalla por separado: es el mismo anuncio, en el mismo lugar, para las
seis pantallas del shell.

## Con o sin cuenta de anuncios

`AdSlot` decide entre dos estados segun `VITE_ADSENSE_CLIENT_ID` y
`VITE_ADSENSE_SLOT_ID`, fijadas al construir la imagen -Vite las incrusta en el
bundle; no se leen en tiempo de ejecucion, asi que cambiarlas exige reconstruir-.

| Configuracion | Que se dibuja | `data-testid` |
|---|---|---|
| Vacias (el caso normal en desarrollo) | Un marcador que reserva el espacio, sin ninguna peticion externa | `ads-placeholder` |
| Ambas fijadas | El anuncio real de Google AdSense | `ads-slot` |

Cual de los dos aparece es una prueba de componente (`AdSlot.test.tsx`), no un
escenario de navegador: depende de una variable fijada al construir la imagen,
no de nada que la persona haga dentro de la aplicacion. Los escenarios de abajo
describen DONDE aparece el espacio, no cual de los dos estados dibuja.

---

## Donde aparece

```gherkin
# language: es

@ui @anuncios
Caracteristica: El espacio de anuncio del shell
  Como LUMA
  Quiero mostrar un espacio de anuncio en cada pantalla de la aplicacion
  Para sostenerme sin cobrar por el uso

  @listo @smoke
  Escenario: Aparece al final del resumen financiero
    Dado una cuenta con sesion iniciada
    Cuando visito el resumen financiero
    Entonces el espacio de anuncio aparece al final de la pantalla

  @listo
  Esquema del escenario: Aparece en las demas pantallas del shell
    Dado una cuenta con sesion iniciada
    Cuando visito "<pantalla>"
    Entonces el espacio de anuncio aparece al final de la pantalla

    Ejemplos:
      | pantalla   |
      | Este ciclo |
      | Ingresos   |
      | Gastos     |
      | Ahorros    |
      | Ajustes    |

  @listo @critico
  Escenario: No aparece en la alta guiada
    Dado una cuenta nueva en el asistente de configuracion inicial
    Cuando reviso la pantalla
    Entonces no hay ningun espacio de anuncio

  @listo
  Escenario: No aparece en las pantallas de sesion
    Dado que no he iniciado sesion
    Cuando visito el inicio de sesion, el registro o la recuperacion de contrasena
    Entonces no hay ningun espacio de anuncio
```

La alta guiada queda fuera a proposito: es el unico flujo que una cuenta nueva
tiene que completar si o si, y no es el momento de interrumpirlo. Las pantallas
de sesion quedan fuera porque viven fuera de `AppShell`, igual que la alta
guiada -el espacio nunca se agrego ahi, no se le quito despues-.

---

## Cobertura automatizada hoy

| Prueba | Que cubre |
|---|---|
| `AdSlot.test.tsx` | Los dos estados del componente: el marcador sin cuenta configurada, y el anuncio real con ambas variables fijadas |

Los escenarios de arriba -donde aparece el espacio, y donde no- todavia no
tienen automatizacion de navegador en este proyecto, igual que el resto de
`@listo` sin runner: describen un comportamiento verificable hoy a mano, a la
espera de que exista el proyecto de automatizacion.
