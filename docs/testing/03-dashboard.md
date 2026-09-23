# Resumen y estados de la interfaz

Lo que hoy muestra el resumen, y cómo se comporta la aplicación cuando la API
tarda o falla.

**Estado:** implementado en las Fases 1 y 1.5. El resumen financiero real llega
en la Fase 10, sobre el motor presupuestal de la Fase 4.

---

## Contenido del resumen

```gherkin
# language: es

@ui @dashboard
Característica: Resumen
  Como usuario con sesión iniciada
  Quiero ver el estado de mi cuenta
  Para saber de un vistazo cómo voy

  Antecedentes:
    Dado que inicié sesión

  @listo @smoke
  Escenario: El resumen me identifica
    Cuando abro el resumen
    Entonces me saluda por mi nombre
    Y muestra el correo con el que inicié sesión

  @listo @smoke @critico
  Escenario: La aplicación confirma que la API responde
    Cuando abro el resumen
    Entonces la tarjeta de conexión indica que la API está respondiendo
    Y muestra la versión del servicio
    Y muestra el perfil activo
    Y muestra la hora del servidor
    # Este escenario recorre la pila completa: navegador, servidor web,
    # Spring Boot y MySQL. Si pasa, la infraestructura está sana.

  @listo
  Escenario: El balance todavía no existe y se dice claramente
    Cuando abro el resumen
    Entonces hay un espacio reservado para el balance
    Y explica que aparecerá cuando registre ciclo, ingresos y gastos
```

---

## Estados de carga, error y vacío

```gherkin
# language: es

@ui @estados
Característica: La aplicación explica qué está pasando
  Como usuario
  Quiero entender si algo está cargando o falló
  Para no quedarme mirando una pantalla en blanco

  Antecedentes:
    Dado que inicié sesión

  @listo
  Escenario: Mientras los datos llegan se muestra un esqueleto
    Dado que la API tarda en responder
    Cuando abro el resumen
    Entonces se muestra un esqueleto con la forma del contenido
    Y no se muestra un contenido a medias
    # Esqueletos y no un indicador giratorio: la pantalla no salta cuando
    # llegan los datos.

  @listo @critico
  Escenario: Cuando la API falla se ofrece reintentar
    Dado que la API responde con un error
    Cuando abro el resumen
    Entonces se muestra un mensaje de que no se pudo cargar la sección
    Y hay una opción para reintentar
    Y no se muestra ningún detalle técnico

  @listo
  Escenario: Reintentar vuelve a pedir los datos
    Dado que la API respondió con un error
    Y que estoy viendo el mensaje de error
    Cuando la API vuelve a funcionar
    Y reintento
    Entonces el resumen se muestra con normalidad

  @listo
  Escenario: Un error del servidor incluye una referencia para rastrearlo
    Dado que la API responde con un error del servidor
    Cuando abro el resumen
    Entonces el mensaje incluye una referencia
    Y esa referencia coincide con el identificador de correlación de la petición
    # Es lo que permite que un problema reportado por un usuario lleve directo
    # a la línea de log correspondiente.

  @listo
  Escenario: Sin conexión el mensaje habla de la conexión
    Dado que no hay conexión con la API
    Cuando abro el resumen
    Entonces el mensaje sugiere revisar la conexión
    Y no incluye ninguna referencia técnica
    # Sin respuesta del servidor no hay identificador de correlación que mostrar.

  @listo
  Escenario: Una sección sin datos lo dice con palabras, no con una tabla vacía
    Cuando abro una sección sin información
    Entonces se explica qué va a aparecer ahí
    Y se ofrece la acción para empezar
```

---

## Mapa con el catálogo de selectores

| Concepto en los escenarios | `data-testid` |
|---|---|
| Página del resumen | `dashboard-page` |
| Saludo con el correo | `dashboard-greeting` |
| Tarjeta de conexión | `dashboard-connection-card` |
| Indicador de estado | `dashboard-connection-status` |
| Versión de la API | `dashboard-api-version` |
| Esqueleto de carga | `state-loading` |
| Estado de error | `state-error` |
| Botón de reintentar | `state-error-retry` |
| Estado vacío | `state-empty` |
