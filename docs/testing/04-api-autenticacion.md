# Contrato de la API — autenticación

Escenarios que se ejercitan por HTTP directo, sin navegador.

Son los más rápidos y estables de toda la suite: no dependen del renderizado ni
de esperas de interfaz. Conviene que cubran el contrato completo y dejar a la
interfaz solo lo que de verdad es visual.

**Estado:** implementado en la Fase 1.5a.

---

## Endpoints públicos

```gherkin
# language: es

@api @sistema
Característica: Endpoints públicos
  Como cliente de la API
  Quiero poder consultar el estado del servicio sin credenciales
  Para saber si está disponible

  @listo @smoke
  Escenario: El servicio reporta su estado
    Cuando consulto la salud del servicio
    Entonces la respuesta es 200
    Y el estado es "UP"

  @listo @smoke
  Escenario: El servicio se identifica sin exigir credenciales
    Cuando consulto la información del sistema
    Entonces la respuesta es 200
    Y el cuerpo incluye el nombre, la versión, los perfiles y la hora del servidor

  @listo
  Escenario: La documentación de la API está disponible fuera de producción
    Cuando abro la especificación OpenAPI
    Entonces la respuesta es 200
    Y describe los endpoints de autenticación
```

---

## Registro

```gherkin
# language: es

@api @autenticacion @registro
Característica: Registro por API
  Como cliente de la API
  Quiero crear una cuenta
  Para obtener un token de acceso

  @listo @smoke @critico
  Escenario: Registro exitoso
    Cuando registro una cuenta con un correo nuevo, un nombre y una contraseña de 8 o más caracteres
    Entonces la respuesta es 201
    Y el cuerpo incluye un token de acceso
    Y el tipo de token es "Bearer"
    Y el cuerpo incluye la vigencia del token en segundos
    Y el cuerpo incluye el usuario con su identificador, correo, nombre y estado

  @listo @seguridad @critico
  Escenario: La respuesta nunca incluye la contraseña
    Cuando registro una cuenta
    Entonces la respuesta no contiene la contraseña en claro
    Y no contiene el hash de la contraseña
    Y no contiene el identificador numérico interno del usuario

  @listo @seguridad
  Escenario: El identificador del usuario es un UUID
    Cuando registro una cuenta
    Entonces el identificador del usuario tiene formato UUID
    # Un identificador autoincremental permite enumerar recursos y deja ver
    # cuántos usuarios tiene el producto.

  @listo @critico
  Escenario: Correo duplicado
    Dado que ya existe una cuenta con un correo
    Cuando registro otra cuenta con ese mismo correo
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"

  @listo
  Escenario: El correo no distingue mayúsculas
    Dado que ya existe una cuenta con el correo "rigo@luma.app"
    Cuando registro una cuenta con el correo "RIGO@LUMA.APP"
    Entonces la respuesta es 422
    Y el código de error es "BUSINESS_RULE_VIOLATION"

  @listo
  Esquema del escenario: Validación de campos
    Cuando registro una cuenta con el campo "<campo>" en "<valor>"
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"
    Y la lista de errores incluye el campo "<campo>"

    Ejemplos:
      | campo    | valor           |
      | email    |                 |
      | email    | esto-no-es-mail |
      | name     |                 |
      | password |                 |
      | password | corta           |

  @listo
  Escenario: Varios campos inválidos se reportan juntos
    Cuando registro una cuenta sin correo, sin nombre y sin contraseña
    Entonces la respuesta es 400
    Y la lista de errores incluye los tres campos
    # El cliente puede señalar todos los problemas de una vez, en lugar de
    # hacer que la persona los descubra uno por uno.

  @listo
  Escenario: Un cuerpo mal formado no rompe el servicio
    Cuando envío al registro un cuerpo que no es JSON válido
    Entonces la respuesta es 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta no incluye ninguna traza de la excepción
```

---

## Inicio de sesión

```gherkin
# language: es

@api @autenticacion @login
Característica: Inicio de sesión por API
  Como cliente de la API
  Quiero intercambiar credenciales por un token
  Para consumir los endpoints protegidos

  Antecedentes:
    Dado que existe una cuenta con credenciales conocidas

  @listo @smoke @critico
  Escenario: Inicio de sesión exitoso
    Cuando inicio sesión con las credenciales correctas
    Entonces la respuesta es 200
    Y el cuerpo incluye un token de acceso
    Y el cuerpo incluye el usuario

  @listo @critico
  Escenario: Contraseña incorrecta
    Cuando inicio sesión con la contraseña equivocada
    Entonces la respuesta es 401
    Y el código de error es "UNAUTHORIZED"

  @listo @seguridad @critico
  Escenario: Un correo inexistente responde igual que una contraseña incorrecta
    Cuando inicio sesión con un correo que no existe
    Entonces la respuesta es 401
    Y el mensaje es idéntico al de una contraseña incorrecta
    # Si el mensaje o el código difirieran, se podría averiguar quién tiene
    # cuenta probando correos.

  @listo @seguridad
  Escenario: El tiempo de respuesta no delata si el correo existe
    Cuando mido el tiempo de respuesta de un inicio de sesión con correo inexistente
    Y mido el tiempo de respuesta de un inicio de sesión con contraseña incorrecta
    Entonces los tiempos son comparables
    # Escenario sensible al ruido del ambiente. Conviene ejecutarlo con varias
    # mediciones y comparar medianas, no ejecuciones sueltas.

  @listo
  Escenario: El correo no distingue mayúsculas al entrar
    Dado que existe una cuenta con el correo "rigo@luma.app"
    Cuando inicio sesión con "RIGO@LUMA.APP" y la contraseña correcta
    Entonces la respuesta es 200
```

---

## Endpoints protegidos

```gherkin
# language: es

@api @autenticacion @seguridad
Característica: Protección de los endpoints
  Como producto que maneja información financiera
  Quiero que ningún dato salga sin un token válido
  Para que nadie acceda a lo que no le corresponde

  @listo @smoke @critico
  Escenario: Con un token válido se obtiene la sesión actual
    Dado que inicié sesión y tengo un token
    Cuando consulto la sesión actual
    Entonces la respuesta es 200
    Y el usuario devuelto es el que inició sesión

  @listo @critico
  Escenario: Sin token no se accede
    Cuando consulto la sesión actual sin enviar token
    Entonces la respuesta es 401

  @listo @seguridad
  Esquema del escenario: Tokens inválidos
    Cuando consulto la sesión actual con un token "<tipo>"
    Entonces la respuesta es 401

    Ejemplos:
      | tipo                          |
      | vacío                         |
      | con texto arbitrario          |
      | con la firma alterada         |
      | firmado con otro secreto      |
      | vencido                       |

  @listo @seguridad
  Escenario: El token no revela el identificador interno
    Dado que inicié sesión y tengo un token
    Cuando examino el contenido del token
    Entonces el sujeto es el UUID del usuario
    Y no aparece el identificador numérico interno
```

---

## Contrato de errores y trazabilidad

```gherkin
# language: es

@api @contrato
Característica: Formato de errores
  Como cliente de la API
  Quiero que todos los errores tengan la misma forma
  Para no tener que manejar cada uno de manera distinta

  @listo @critico
  Escenario: Todo error trae la misma estructura
    Cuando provoco cualquier error de la API
    Entonces la respuesta incluye el estado, el título, el detalle y la ruta
    Y incluye la marca de tiempo
    Y incluye un código de error estable
    Y incluye un identificador de rastreo

  @listo
  Escenario: El identificador de correlación se propaga
    Cuando envío una petición con un identificador de correlación propio
    Entonces la respuesta devuelve ese mismo identificador en la cabecera
    Y si la petición falla, el identificador de rastreo del cuerpo coincide

  @listo
  Escenario: Sin identificador propio el servicio genera uno
    Cuando envío una petición sin identificador de correlación
    Entonces la respuesta incluye un identificador generado por el servicio

  @listo @seguridad @critico
  Escenario: Los errores no filtran detalles internos
    Cuando provoco un error del servidor
    Entonces la respuesta no incluye trazas de excepción
    Y no incluye nombres de clases ni de tablas
    Y no incluye sentencias SQL

  @listo
  Escenario: Una ruta inexistente responde con el mismo formato
    Cuando consulto una ruta de la API que no existe
    Entonces la respuesta es 404
    Y el código de error es "RESOURCE_NOT_FOUND"
    Y el cuerpo tiene la misma estructura que cualquier otro error

  @listo
  Escenario: Un método no permitido responde con el mismo formato
    Cuando consulto con GET un endpoint que solo acepta POST
    Entonces la respuesta es 405
    Y el cuerpo tiene la misma estructura que cualquier otro error
```

---

## Referencia rápida de endpoints

| Método | Ruta | Requiere token |
|---|---|---|
| POST | `/api/v1/auth/register` | No |
| POST | `/api/v1/auth/login` | No |
| GET | `/api/v1/auth/me` | Sí |
| GET | `/api/v1/system/info` | No |
| GET | `/actuator/health` | No |

Códigos de error definidos en [`../api.md`](../api.md).
