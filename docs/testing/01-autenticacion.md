# Autenticación

Registro, inicio de sesión, cierre de sesión y protección de rutas.

**Estado:** implementado en la Fase 1.5.

---

## Una nota que cambia cómo se automatiza esto

El access token vive **solo en memoria**, nunca en `localStorage`. Lo que
sobrevive a una recarga es el token de renovación, en una cookie HttpOnly que el
JavaScript de la página no puede leer.

Consecuencias para la automatización:

- **No se puede sembrar el token en el almacenamiento del navegador** para
  saltarse el login. Hay que pasar por el formulario.
- **Sí se puede reutilizar el estado de sesión entre escenarios** guardando el
  contexto del navegador: la cookie basta para que la aplicación se reconstruya
  la sesión sola al arrancar.
- **Cada renovación rota la cookie.** Un contexto guardado y reutilizado mucho
  después puede llevar un token ya rotado. Conviene regenerarlo por ejecución en
  lugar de conservarlo entre corridas.
- Al recargar hay un instante de pantalla de arranque
  (`auth-boot-splash`) antes de que se decida si hay sesión. Esperar por ella, o
  por el contenido final, evita pruebas intermitentes.

---

## Registro

```gherkin
# language: es

@ui @autenticacion @registro
Característica: Registro de una cuenta nueva
  Como alguien que quiere ordenar sus finanzas
  Quiero crear una cuenta en LUMA
  Para empezar a armar mi presupuesto

  Antecedentes:
    Dado que estoy en la pantalla de registro

  @listo @smoke @critico
  Escenario: Crear una cuenta con datos válidos
    Cuando registro la cuenta con un correo que nadie ha usado
    Y una contraseña de al menos 8 caracteres
    Entonces entro directo al resumen sin pasar por el inicio de sesión
    Y el resumen me saluda por mi nombre
    Y el encabezado muestra mi nombre

  @listo @critico
  Escenario: El correo ya está registrado
    Dado que ya existe una cuenta con el correo "rigo@luma.app"
    Cuando intento registrarme con ese mismo correo
    Entonces el formulario muestra un aviso de que ya existe una cuenta con ese correo
    Y sigo en la pantalla de registro

  @listo
  Escenario: La contraseña es demasiado corta
    Cuando intento registrarme con la contraseña "corta"
    Entonces el campo de contraseña muestra que debe tener al menos 8 caracteres
    Y el aviso aparece bajo ese campo, no como alerta general
    Y sigo en la pantalla de registro

  @listo
  Esquema del escenario: Campos obligatorios y mal formados
    Cuando intento registrarme con el campo "<campo>" en "<valor>"
    Entonces ese campo muestra un mensaje de error
    Y sigo en la pantalla de registro

    Ejemplos:
      | campo      | valor           |
      | nombre     |                 |
      | correo     |                 |
      | correo     | esto-no-es-mail |
      | contraseña |                 |

  @listo
  Escenario: El formulario no se puede enviar dos veces
    Cuando envío el formulario de registro con datos válidos
    Entonces el botón queda deshabilitado mientras se procesa
    Y muestra que está trabajando

  @listo
  Escenario: Ir al inicio de sesión desde el registro
    Cuando sigo el enlace para entrar con una cuenta existente
    Entonces llego a la pantalla de inicio de sesión
```

---

## Inicio de sesión

```gherkin
# language: es

@ui @autenticacion @login
Característica: Inicio de sesión
  Como usuario registrado
  Quiero entrar a mi cuenta
  Para ver cómo va mi ciclo

  Antecedentes:
    Dado que existe una cuenta con el correo "rigo@luma.app" y contraseña "luma12345"
    Y que estoy en la pantalla de inicio de sesión

  @listo @smoke @critico
  Escenario: Entrar con credenciales correctas
    Cuando entro con "rigo@luma.app" y "luma12345"
    Entonces llego al resumen
    Y el resumen me saluda por mi nombre
    Y el resumen muestra el correo con el que inicié sesión

  @listo @critico
  Escenario: La contraseña es incorrecta
    Cuando entro con "rigo@luma.app" y "contraseña-equivocada"
    Entonces el formulario muestra una alerta general de credenciales incorrectas
    Y sigo en la pantalla de inicio de sesión

  @listo @seguridad
  Escenario: Un correo que no existe da el mismo mensaje que una contraseña incorrecta
    Cuando entro con "nadie@luma.app" y "luma12345"
    Entonces el mensaje es exactamente el mismo que cuando la contraseña es incorrecta
    # El mensaje no debe distinguir entre "no existe esa cuenta" y "contraseña
    # incorrecta": si lo hiciera, se podría averiguar quién tiene cuenta en LUMA
    # probando correos.

  @listo
  Esquema del escenario: Campos vacíos
    Cuando entro con el correo "<correo>" y la contraseña "<contraseña>"
    Entonces el formulario señala el campo faltante
    Y sigo en la pantalla de inicio de sesión

    Ejemplos:
      | correo         | contraseña |
      |                | luma12345  |
      | rigo@luma.app  |            |
      |                |            |

  @listo
  Escenario: Ir al registro desde el inicio de sesión
    Cuando sigo el enlace para crear una cuenta
    Entonces llego a la pantalla de registro
```

---

## Rutas protegidas y cierre de sesión

```gherkin
# language: es

@ui @autenticacion @rutas-protegidas
Característica: Acceso a las secciones de la aplicación
  Como producto que maneja información financiera
  Quiero que ninguna pantalla se muestre sin sesión
  Para que nadie vea datos que no le corresponden

  @listo @smoke @critico
  Escenario: Sin sesión no se entra al resumen
    Dado que no he iniciado sesión
    Cuando abro la raíz de la aplicación
    Entonces llego a la pantalla de inicio de sesión

  @listo
  Esquema del escenario: Ninguna sección es accesible sin sesión
    Dado que no he iniciado sesión
    Cuando abro directamente la sección "<seccion>"
    Entonces llego a la pantalla de inicio de sesión

    Ejemplos:
      | seccion           |
      | ingresos          |
      | gastos fijos      |
      | gastos variables  |
      | ahorros           |
      | ajustes           |

  @listo @critico
  Escenario: Después de entrar vuelvo a donde iba
    Dado que no he iniciado sesión
    Y que intenté abrir la sección de ahorros
    Cuando entro con credenciales válidas
    Entonces llego a la sección de ahorros, no al resumen

  @listo @smoke
  Escenario: Cerrar sesión
    Dado que inicié sesión
    Cuando cierro la sesión desde el encabezado
    Entonces llego a la pantalla de inicio de sesión
    Y al intentar volver al resumen me regresa al inicio de sesión

  @listo @critico
  Escenario: Al recargar la página la sesión continúa
    Dado que inicié sesión
    Cuando recargo la página
    Entonces sigo dentro, en la misma sección
    Y no vuelvo a ver la pantalla de inicio de sesión
    # El token de acceso se pierde al recargar porque vive en memoria. Lo que
    # sobrevive es la cookie de renovación, con la que la aplicación consigue
    # uno nuevo antes de pintar nada.

  @listo
  Escenario: Mientras se decide si hay sesión no parpadea el inicio de sesión
    Dado que inicié sesión
    Cuando recargo la página
    Entonces se muestra brevemente la pantalla de arranque
    Y en ningún momento aparece el formulario de inicio de sesión

  @listo @critico
  Escenario: El token de acceso vencido se renueva solo
    Dado que inicié sesión
    Y que el token de acceso venció
    Cuando navego a otra sección
    Entonces la sección carga con normalidad
    Y no se me pide iniciar sesión de nuevo
    # La renovación es invisible: la petición que recibió 401 se reintenta sola
    # con el token nuevo.

  @listo @seguridad
  Escenario: Cerrar sesión revoca también el token de renovación
    Dado que inicié sesión
    Cuando cierro la sesión
    Y recargo la página
    Entonces llego a la pantalla de inicio de sesión
    # Si la cookie siguiera siendo válida, recargar devolvería al usuario dentro.

  @listo @seguridad
  Escenario: Dos pestañas abiertas no se expulsan entre sí
    Dado que inicié sesión en una pestaña
    Y que abro la aplicación en una segunda pestaña
    Entonces ambas pestañas quedan con sesión iniciada
    # Ambas intentan renovar casi a la vez con la misma cookie. El backend
    # distingue esa carrera de un reuso malicioso por un margen de 30 segundos.

  @listo @seguridad
  Escenario: El token nunca se guarda en el navegador
    Dado que inicié sesión
    Entonces el almacenamiento local del navegador no contiene el token
    Y el almacenamiento de sesión tampoco
    # Lo único que se persiste es la preferencia de tema, bajo la clave "luma-ui".

  @listo @seguridad
  Escenario: Un token vencido devuelve al inicio de sesión
    Dado que inicié sesión
    Y que el token deja de ser válido
    Cuando la aplicación hace una petición a la API
    Entonces se cierra la sesión
    Y llego a la pantalla de inicio de sesión
```

---

## Mapa con el catálogo de selectores

| Concepto en los escenarios | `data-testid` |
|---|---|
| Pantalla de inicio de sesión | `auth-login-page` |
| Pantalla de registro | `auth-register-page` |
| Campo nombre / correo / contraseña | `auth-name-input` · `auth-email-input` · `auth-password-input` |
| Botón de envío | `auth-submit-button` |
| Alerta general | `auth-form-error` |
| Enlace a registro / a inicio de sesión | `auth-go-to-register` · `auth-go-to-login` |
| Nombre en el encabezado | `auth-user-name` |
| Cerrar sesión | `auth-logout-button` |
| Pantalla de arranque | `auth-boot-splash` |
| Saludo del resumen | `dashboard-greeting` |

Los errores por campo no tienen `data-testid` propio: aparecen como texto de
ayuda del input, así que se leen desde el campo.

Catálogo completo en [`../testids.md`](../testids.md).
