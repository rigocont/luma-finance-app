# Recuperación y cambio de contraseña

**Estado:** implementado en la Fase 2b. Backend e interfaz, ambos automatizables.

---

## Cómo verificar el correo

En local los mensajes no salen a ningún destinatario: los captura **Mailpit**.

| Qué | Dónde |
|---|---|
| Bandeja web | http://localhost:8025 |
| API de Mailpit | http://localhost:8025/api/v1/messages |

Para automatizar, la API de Mailpit es lo útil: se consulta el último mensaje,
se extrae el enlace del cuerpo y se sigue. Es lo que convierte este flujo en
algo probable de punta a punta sin intervención manual.

En el perfil `local` el enlace también se escribe en el log de la aplicación
(`luma.mail.log-links: true`), lo que sirve para depurar a mano. **En producción
esa opción está apagada**: un log con enlaces de recuperación es una vía de
acceso a las cuentas.

---

## Solicitar el enlace

```gherkin
# language: es

@api @contrasena @recuperacion
Característica: Solicitar recuperación de contraseña
  Como usuario que olvidó su contraseña
  Quiero recibir un enlace para elegir una nueva
  Para recuperar el acceso a mi cuenta

  @listo @smoke @critico
  Escenario: Solicitud con un correo registrado
    Dado que existe una cuenta con el correo "rigo@luma.app"
    Cuando pido recuperar la contraseña de "rigo@luma.app"
    Entonces la respuesta es 202
    Y llega un correo a "rigo@luma.app"
    Y el correo contiene un enlace de recuperación
    Y el enlace apunta a la aplicación, no a la API

  @listo @seguridad @critico
  Escenario: Un correo no registrado responde igual
    Cuando pido recuperar la contraseña de "nadie@luma.app"
    Entonces la respuesta es 202
    Y no llega ningún correo
    # La respuesta es idéntica al caso anterior a propósito. Si difiriera,
    # cualquiera podría averiguar quién tiene cuenta en LUMA probando correos.

  @listo @seguridad
  Escenario: Pedirlo muchas veces no manda muchos correos
    Dado que existe una cuenta con el correo "rigo@luma.app"
    Y que acabo de pedir recuperar esa contraseña
    Cuando vuelvo a pedirlo de inmediato
    Entonces la respuesta es 202
    Y sigue habiendo un solo correo en la bandeja
    # Evita llenar el buzón de la persona y que el endpoint sirva de
    # amplificador para mandar correo a terceros.

  @listo
  Esquema del escenario: Validación del correo
    Cuando pido recuperar la contraseña de "<correo>"
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"

    Ejemplos:
      | correo          |
      |                 |
      | esto-no-es-mail |

  @listo
  Escenario: El correo no revela información de la cuenta
    Dado que existe una cuenta con el correo "rigo@luma.app"
    Cuando pido recuperar esa contraseña
    Y leo el mensaje recibido
    Entonces el cuerpo no contiene la contraseña
    Y no contiene el hash de la contraseña
    Y aclara qué hacer si la solicitud no fue suya
```

---

## Usar el enlace

```gherkin
# language: es

@api @contrasena @recuperacion
Característica: Elegir una contraseña nueva
  Como usuario que recibió el enlace
  Quiero establecer una contraseña nueva
  Para volver a entrar

  Antecedentes:
    Dado que existe una cuenta con el correo "rigo@luma.app"
    Y que pedí recuperar la contraseña
    Y que obtuve el código del enlace recibido

  @listo @smoke @critico
  Escenario: Restablecer con un código válido
    Cuando establezco la contraseña nueva "nueva-clave-99"
    Entonces la respuesta es 204
    Y puedo iniciar sesión con "nueva-clave-99"
    Y ya no puedo iniciar sesión con la contraseña anterior

  @listo @seguridad @critico
  Escenario: El código sirve una sola vez
    Dado que ya usé el código para establecer una contraseña
    Cuando intento usar el mismo código otra vez
    Entonces la respuesta es 422
    Y el mensaje indica que el enlace ya no es válido

  @listo @seguridad @critico
  Escenario: Restablecer cierra todas las sesiones abiertas
    Dado que tengo una sesión iniciada en otro dispositivo
    Cuando establezco una contraseña nueva con el código
    Entonces la sesión del otro dispositivo deja de funcionar
    Y su token de renovación ya no sirve
    # Quien restablece su contraseña suele hacerlo porque teme que alguien más
    # tenga acceso. Dejar sesiones abiertas haría inútil el cambio.

  @listo @seguridad
  Escenario: Un código vencido no sirve
    Dado que el código de recuperación venció
    Cuando intento establecer una contraseña nueva
    Entonces la respuesta es 422

  @listo @seguridad
  Esquema del escenario: Códigos inválidos
    Cuando intento establecer una contraseña nueva con un código "<tipo>"
    Entonces la respuesta es <estado>

    Ejemplos:
      | tipo                  | estado |
      | vacío                 | 400    |
      | con texto arbitrario  | 422    |
      | de otro usuario ya usado | 422 |

  @listo
  Escenario: La contraseña nueva debe cumplir el mínimo
    Cuando intento establecer la contraseña "corta"
    Entonces la respuesta es 400
    Y la lista de errores incluye el campo "newPassword"
```

---

## Cambiar la contraseña estando dentro

```gherkin
# language: es

@api @contrasena @cambio
Característica: Cambiar la contraseña desde la aplicación
  Como usuario con sesión iniciada
  Quiero cambiar mi contraseña
  Para mantener mi cuenta segura

  Antecedentes:
    Dado que inicié sesión con la contraseña "luma12345"

  @listo @smoke @critico
  Escenario: Cambio exitoso
    Cuando cambio la contraseña de "luma12345" a "otra-clave-77"
    Entonces la respuesta es 204
    Y puedo iniciar sesión con "otra-clave-77"
    Y ya no puedo iniciar sesión con "luma12345"

  @listo @seguridad @critico
  Escenario: Se exige la contraseña actual
    Cuando intento cambiar la contraseña dando una contraseña actual equivocada
    Entonces la respuesta es 401
    Y mi contraseña sigue siendo la anterior
    # Si no se exigiera, una sesión olvidada abierta en una computadora ajena
    # permitiría apropiarse de la cuenta.

  @listo
  Escenario: La contraseña nueva debe ser distinta
    Cuando intento cambiar la contraseña por la misma que ya tengo
    Entonces la respuesta es 422
    Y el mensaje indica que debe ser distinta de la actual

  @listo @seguridad @critico
  Escenario: Cambiar la contraseña cierra todas las sesiones
    Dado que tengo sesión iniciada en dos dispositivos
    Cuando cambio la contraseña en uno de ellos
    Entonces ninguna de las dos sesiones sigue funcionando
    Y hay que volver a iniciar sesión
    # Incluye el dispositivo donde se hizo el cambio. Es deliberado: más simple
    # de razonar y no deja ninguna sesión viva por error.

  @listo @seguridad
  Escenario: Cambiar la contraseña invalida los enlaces de recuperación pendientes
    Dado que pedí un enlace de recuperación
    Cuando cambio la contraseña desde la aplicación
    Y luego intento usar ese enlace
    Entonces la respuesta es 422

  @listo @seguridad
  Escenario: Sin sesión no se puede cambiar la contraseña
    Dado que no he iniciado sesión
    Cuando intento cambiar la contraseña
    Entonces la respuesta es 401

  @listo
  Esquema del escenario: Campos obligatorios
    Cuando intento cambiar la contraseña con "<campo>" vacío
    Entonces la respuesta es 400
    Y la lista de errores incluye ese campo

    Ejemplos:
      | campo           |
      | currentPassword |
      | newPassword     |
```

---

## Las pantallas

```gherkin
# language: es

@ui @contrasena @recuperacion
Característica: Recuperar el acceso desde la aplicación
  Como usuario que olvidó su contraseña
  Quiero pedir y usar un enlace de recuperación
  Para volver a entrar sin ayuda de nadie

  @listo @smoke
  Escenario: Llegar a la recuperación desde el inicio de sesión
    Dado que estoy en la pantalla de inicio de sesión
    Cuando sigo el enlace de contraseña olvidada
    Entonces llego a la pantalla de recuperación

  @listo @smoke @critico
  Escenario: Pedir el enlace
    Dado que estoy en la pantalla de recuperación
    Cuando pido el enlace para "rigo@luma.app"
    Entonces la pantalla confirma que el correo va en camino
    Y muestra el correo al que se envió
    Y explica que el enlace vence en una hora y sirve una sola vez

  @listo @seguridad
  Escenario: La confirmación es idéntica con un correo no registrado
    Dado que estoy en la pantalla de recuperación
    Cuando pido el enlace para un correo sin cuenta
    Entonces la pantalla confirma lo mismo que con un correo registrado
    # La interfaz tampoco debe delatar qué correos tienen cuenta.

  @listo @smoke @critico
  Escenario: Establecer la contraseña nueva desde el enlace
    Dado que pedí recuperar mi contraseña
    Y que abrí el enlace que llegó al correo
    Cuando escribo la contraseña nueva y la confirmo
    Entonces llego a la pantalla de inicio de sesión
    Y se muestra un aviso de que la contraseña quedó lista
    Y puedo entrar con la contraseña nueva

  @listo
  Escenario: Las contraseñas no coinciden
    Dado que abrí un enlace de recuperación válido
    Cuando escribo contraseñas distintas en los dos campos
    Entonces el campo de confirmación indica que no coinciden
    Y no se envía nada al servidor

  @listo
  Escenario: Abrir la pantalla sin código
    Cuando abro la ruta de contraseña nueva sin código en la dirección
    Entonces la pantalla indica que el enlace no sirve
    Y ofrece pedir un enlace nuevo
    Y esa opción lleva a la pantalla de recuperación

  @listo
  Escenario: Un código ya usado
    Dado que ya usé un enlace de recuperación
    Cuando vuelvo a abrir ese mismo enlace
    Y escribo una contraseña nueva
    Entonces el formulario indica que el enlace ya no es válido

@ui @contrasena @cambio @ajustes
Característica: Cambiar la contraseña desde Ajustes
  Como usuario con sesión iniciada
  Quiero cambiar mi contraseña
  Para mantener mi cuenta segura

  Antecedentes:
    Dado que inicié sesión
    Y que estoy en Ajustes

  @listo
  Escenario: Ajustes muestra los datos de la cuenta
    Entonces se muestra mi nombre y mi correo
    Y hay una sección de apariencia
    Y hay una sección de contraseña

  @listo @smoke @critico
  Escenario: El cambio exitoso cierra la sesión
    Cuando cambio mi contraseña dando la actual y una nueva
    Entonces llego a la pantalla de inicio de sesión
    Y se muestra un aviso de que la contraseña cambió
    Y al recargar sigo fuera
    # El backend cerró todas las sesiones, incluida esta. La cookie de
    # renovación tampoco sirve ya.

  @listo @seguridad
  Escenario: La contraseña actual equivocada no cambia nada
    Cuando intento cambiarla dando una contraseña actual equivocada
    Entonces el formulario muestra un error
    Y sigo dentro de la aplicación
    Y mi contraseña sigue siendo la anterior

  @listo
  Escenario: La confirmación debe coincidir
    Cuando escribo una confirmación distinta de la contraseña nueva
    Entonces el campo de confirmación indica que no coinciden
    Y no se envía nada al servidor

  @listo
  Escenario: La contraseña nueva debe ser distinta de la actual
    Cuando intento poner la misma contraseña que ya tengo
    Entonces el formulario indica que debe ser distinta
    Y sigo dentro de la aplicación

  @listo
  Esquema del escenario: Elegir el tema desde Ajustes
    Cuando elijo el tema "<tema>"
    Entonces la aplicación se muestra con ese tema
    Y la opción elegida queda marcada
    Y la preferencia sobrevive a una recarga

    Ejemplos:
      | tema            |
      | Claro           |
      | Oscuro          |
      | Como el sistema |
```

---

## Mapa con el catálogo de selectores

| Concepto en los escenarios | `data-testid` |
|---|---|
| Enlace de contraseña olvidada | `auth-go-to-forgot-password` |
| Pantalla de recuperación | `auth-forgot-password-page` |
| Confirmación de envío | `auth-reset-link-sent` |
| Pantalla de contraseña nueva | `auth-reset-password-page` |
| Enlace inválido | `auth-invalid-reset-link` |
| Pedir un enlace nuevo | `auth-request-new-link` |
| Aviso en el inicio de sesión | `auth-login-notice` |
| Campos de contraseña | `auth-current-password-input`, `auth-new-password-input`, `auth-confirm-password-input` |
| Página de Ajustes | `settings-page` |
| Tarjeta de contraseña | `settings-change-password-card` |
| Enviar el cambio | `settings-change-password-submit` |
| Opciones de tema | `settings-theme-light`, `settings-theme-dark`, `settings-theme-system` |

---

## Referencia de endpoints

| Método | Ruta | Requiere token | Respuesta |
|---|---|---|---|
| POST | `/api/v1/auth/password/forgot` | No | 202 siempre |
| POST | `/api/v1/auth/password/reset` | No | 204 |
| POST | `/api/v1/auth/password/change` | Sí | 204 |

`forgot` y `reset` son públicos por definición: se usan justamente cuando no se
puede iniciar sesión.
