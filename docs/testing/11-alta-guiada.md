# Alta guiada

El asistente que configura una cuenta nueva: ciclo, ingresos, gastos y una
primera meta.

**Estado:** API y pantalla en la Fase 9. Todo automatizable hoy.

---

## No hay borrador: lo capturado es real desde el primer paso

Es la decisión que gobierna todo lo demás. Capturar un ingreso en el asistente
**crea el ingreso**, con el mismo endpoint que usa la sección de Ingresos.

| | Con borrador | Sin borrador (lo que hace LUMA) |
|---|---|---|
| Dónde vive lo capturado | Una tabla aparte | En ingresos, gastos y metas |
| Salir a la mitad | No deja nada | Deja lo que capturaste |
| Retomar | Leer el borrador | El servidor ya sabe qué hay |
| Migración nueva | Sí | No |
| Riesgo | Dos modelos que se separan con el tiempo | Cuentas abandonadas con datos |

El último riesgo se atiende con la limpieza de altas abandonadas, más abajo.

Para la automatización esto tiene una consecuencia práctica: **el estado del
asistente se puede montar por la API**. Un escenario que empieza en el paso de
gastos no necesita hacer clic por los anteriores — basta crear un ingreso con
`POST /incomes` y entrar.

---

## Dónde retoma

`resumeStep` se **deduce** de lo que hay, no de un avance guardado. Un paso
guardado mentiría en cuanto la persona borrara algo desde otra pantalla.

| Lo que hay | Retoma en |
|---|---|
| Ningún ingreso | `INCOMES` |
| Al menos un ingreso | `SUMMARY` |

Solo los ingresos son obligatorios, así que solo ellos pueden retener a alguien.
Mandar a «gastos» a quien ya los saltó daría a entender que le falta algo que
decidió no hacer.

---

## El estado del alta

```gherkin
# language: es

@api @alta
Característica: Dónde va la configuración inicial
  Como persona que acaba de crear su cuenta
  Quiero que LUMA recuerde lo que ya hice
  Para no empezar de cero cada vez que vuelvo

  @listo @smoke @critico
  Escenario: Una cuenta nueva no ha terminado ni puede terminar
    Dado que acabo de registrarme
    Cuando consulto el estado del alta
    Entonces no está terminada
    Y todavía no se puede terminar

  @listo @critico
  Escenario: Sin ingresos se retoma en el paso de ingresos
    Cuando consulto el estado del alta
    Entonces el paso sugerido es el de ingresos

  @listo @critico
  Escenario: Con un ingreso ya se puede terminar
    Dado que capturé un ingreso
    Cuando consulto el estado del alta
    Entonces ya se puede terminar
    Y el paso sugerido es el resumen

  @listo @critico
  Escenario: El conteo sale de los datos reales
    Dado que creé dos ingresos por la API, sin pasar por el asistente
    Cuando consulto el estado del alta
    Entonces cuenta dos ingresos

  @listo @seguridad
  Escenario: El estado exige sesión
    Dado que no estoy autenticado
    Cuando consulto el estado del alta
    Entonces la respuesta es 401
```

---

## Terminar

```gherkin
# language: es

@api @alta @ciclos
Característica: Terminar la configuración inicial
  Como persona que ya capturó lo suyo
  Quiero empezar a usar LUMA
  Para ver cuánto me queda

  @listo @smoke @critico
  Escenario: Terminar marca la cuenta y abre el primer ciclo
    Dado que capturé un ingreso
    Cuando termino el alta
    Entonces la cuenta queda configurada
    Y existe un ciclo en curso

  @listo @critico
  Escenario: El primer ciclo ya trae lo capturado
    Dado que capturé un ingreso
    Cuando termino el alta
    Entonces el ciclo tiene renglones

  @listo @critico
  Escenario: Sin ingresos se rechaza
    Dado que no capturé ningún ingreso
    Cuando intento terminar el alta
    Entonces la respuesta es 422
    Y la cuenta sigue sin configurar

  @listo @critico
  Escenario: Terminar dos veces se rechaza
    Dado que ya terminé el alta
    Cuando intento terminarla otra vez
    Entonces la respuesta es 422
    Y no se abre un segundo ciclo
```

---

## Hacerlo después

```gherkin
# language: es

@api @alta
Característica: Posponer la configuración
  Como persona que solo quería mirar
  Quiero entrar sin configurar nada
  Para decidir con calma

  @listo @critico
  Escenario: Posponer marca la cuenta sin abrir ciclo
    Cuando pospongo el alta
    Entonces la cuenta queda configurada
    Y no existe ningún ciclo

  @listo @critico
  Escenario: Lo capturado se conserva
    Dado que capturé un ingreso
    Cuando pospongo el alta
    Entonces el ingreso sigue ahí

  @listo
  Escenario: Posponer dos veces no falla
    Dado que ya pospuse el alta
    Cuando lo pospongo otra vez
    Entonces la respuesta es 204
```

---

## La limpieza de altas abandonadas

Guardar de verdad desde el primer paso tiene un costo: una cuenta que nunca
terminó queda con datos sueltos. Un trabajo diario los **invalida**.

**Por qué no hace falta una columna nueva.** El asistente es obligatorio:
mientras `onboarding_completed_at` sea nulo, ninguna otra pantalla es
alcanzable. De ahí se sigue que todo lo que tiene una cuenta en ese estado se
capturó en el asistente. La marca que haría falta no existe porque **la ausencia
de esa fecha ya es la marca**.

**Invalida, no borra.** Usa el borrado lógico que ingresos, gastos y metas ya
tenían. Un `DELETE` de verdad dejaría a quien vuelve sin forma de entender qué
pasó, y a nosotros sin forma de revisarlo si alguien reclama.

**Se mide desde la última captura, no desde el registro.** Quien se registró
hace un mes y empezó hoy no se toca.

**Ante la duda, no toca nada.** Si no se puede saber cuándo se capturó algo, la
cuenta se deja en paz. Equivocarse invalidando datos de alguien que sigue
trabajando es mucho peor que dejar una cuenta sin limpiar un día más.

Plazo: `LUMA_ONBOARDING_ABANDON_AFTER`, una semana por omisión. En `0` se apaga.

```gherkin
# language: es

@api @alta @programado
Característica: Limpieza de altas abandonadas
  Como dueño del producto
  Quiero que lo capturado en un alta que nadie terminó deje de contar
  Para que la base no se llene de cuentas a medias

  @listo @critico
  Escenario: Invalida lo capturado en un alta abandonada
    Dado un alta sin terminar cuya última captura fue hace más del plazo
    Cuando corre la limpieza
    Entonces sus ingresos, gastos y metas dejan de aparecer

  @listo @critico
  Escenario: Invalida, no borra
    Dado un alta abandonada
    Cuando corre la limpieza
    Entonces las filas siguen existiendo en la base
    Y la cuenta sigue pudiendo entrar

  @listo @critico
  Escenario: No toca a quien capturó algo hace poco
    Dado un alta sin terminar con una captura de ayer
    Cuando corre la limpieza
    Entonces no se invalida nada

  @listo @critico
  Escenario: No toca una cuenta que ya terminó el alta
    Dado una cuenta configurada hace un año
    Cuando corre la limpieza
    Entonces no se invalida nada

  @listo @critico
  Escenario: No toca una cuenta que pospuso el alta
    Dado una cuenta que eligió "hacerlo después"
    Cuando corre la limpieza
    Entonces lo que había capturado sigue ahí

  @listo
  Escenario: Con el plazo en cero la limpieza está apagada
    Dado el plazo configurado en cero
    Cuando corre la limpieza
    Entonces no se invalida nada

  @listo
  Escenario: Después de limpiar, el asistente empieza de nuevo
    Dado un alta que se limpió
    Cuando la persona vuelve a entrar
    Entonces el asistente la manda al paso de ingresos
```

---

## La pantalla

```gherkin
# language: es

@ui @alta
Característica: El asistente
  Como persona que acaba de crear su cuenta
  Quiero que me guíen para configurarla
  Para no enfrentarme a una aplicación vacía

  @listo @smoke @critico
  Escenario: Una cuenta nueva entra directo al asistente
    Cuando creo una cuenta
    Entonces llego al asistente

  @listo @critico
  Escenario: Cualquier ruta manda al asistente hasta terminarlo
    Dado una cuenta sin configurar
    Cuando intento abrir la sección de Gastos
    Entonces llego al asistente

  @listo @critico
  Escenario: El asistente no tiene menú lateral
    Cuando estoy en el asistente
    Entonces no veo la barra de navegación

  @listo @critico
  Escenario: Una cuenta que ya lo completó no vuelve a verlo
    Dado una cuenta configurada
    Cuando abro la ruta del asistente
    Entonces me manda al resumen

  @listo @critico
  Escenario: No se puede avanzar sin al menos un ingreso
    Dado que estoy en el paso de ingresos y no capturé ninguno
    Entonces el botón de continuar está deshabilitado
    Y se explica por qué

  @listo @critico
  Escenario: Al capturar el primer ingreso se habilita continuar
    Dado que estoy en el paso de ingresos
    Cuando agrego un ingreso
    Entonces el botón de continuar se habilita
    Y el ingreso aparece en la lista

  @listo
  Escenario: Un error de validación conserva lo capturado
    Cuando agrego un ingreso sin nombre
    Entonces el error aparece junto al campo
    Y el monto que escribí sigue ahí

  @listo @critico
  Escenario: Avanzar y retroceder conserva lo capturado
    Dado que capturé un ingreso y avancé a gastos
    Cuando regreso al paso de ingresos
    Entonces el ingreso sigue en la lista

  @listo @critico
  Escenario: Salir a la mitad y volver retoma donde se quedó
    Dado que capturé un ingreso y cerré la pestaña
    Cuando vuelvo a entrar
    Entonces el asistente me lleva al resumen
    Y mi ingreso sigue ahí

  @listo @critico
  Escenario: Elegir un gasto del catálogo propone su nombre y su tipo
    Cuando elijo la categoría "Despensa"
    Entonces el nombre se propone como "Despensa"
    Y se indica que su monto cambia

  @listo
  Escenario: Agregar un gasto que no está en el catálogo
    Cuando escribo un nombre sin elegir categoría
    Y pongo un monto y agrego
    Entonces el gasto aparece en la lista sin categoría

  @listo @critico
  Escenario: Los pasos opcionales se pueden saltar
    Dado que estoy en el paso de gastos
    Entonces existe un botón para saltarlo

  @listo
  Escenario: El paso de ingresos NO se puede saltar
    Dado que estoy en el paso de ingresos
    Entonces no hay botón para saltarlo

  @listo @critico
  Escenario: El resumen muestra lo que quedó configurado
    Dado que capturé un ingreso y dos gastos
    Cuando llego al resumen
    Entonces dice "1 ingreso" y "2 gastos"
    Y muestra cada cuánto presupuesto

  @listo
  Escenario: El resumen avisa si no hay gastos
    Dado que salté el paso de gastos
    Cuando llego al resumen
    Entonces se explica que se pueden agregar después

  @listo @smoke @critico
  Escenario: Al terminar se crea el primer ciclo y se llega al resumen
    Dado que capturé un ingreso
    Cuando presiono "Empezar"
    Entonces llego al resumen financiero
    Y aparece un aviso de que el ciclo está listo
    Y el menú lateral ya se ve

  @listo @critico
  Escenario: "Hacerlo después" entra sin configurar nada
    Cuando presiono "Hacerlo después"
    Entonces llego al resumen financiero
    Y no vuelvo a ver el asistente

  @listo @responsive
  Escenario: En teléfono el avance se dice con texto, no con el stepper
    Dado un viewport de teléfono
    Cuando estoy en el asistente
    Entonces veo "Paso 2 de 5" en lugar de la barra de pasos
```

---

## Cambiar el ciclo despues, desde Ajustes

La misma preferencia que se elige en el primer paso del asistente se puede
cambiar luego en Ajustes. Es el mismo endpoint.

```gherkin
# language: es

@ui @ajustes
Característica: Cambiar el ciclo presupuestal
  Como persona a la que le cambiaron las fechas de cobro
  Quiero ajustar cada cuánto presupuesto
  Para que los ciclos coincidan con mi realidad

  @listo @critico
  Escenario: La tarjeta muestra el ciclo que tengo
    Dado que elegí ciclo mensual en el alta
    Cuando abro Ajustes
    Entonces la tarjeta del ciclo dice "Cada mes"

  @listo
  Escenario: Guardar está deshabilitado si no cambié nada
    Cuando abro Ajustes
    Entonces el botón de guardar el ciclo está deshabilitado

  @listo @critico
  Escenario: Cambiar el tipo de ciclo lo guarda
    Cuando cambio a quincenal y guardo
    Entonces aparece un aviso de que aplica al siguiente ciclo

  @listo @critico
  Escenario: El ciclo abierto NO cambia de fechas
    Dado un ciclo en curso del 1 al 15
    Cuando cambio el tipo de ciclo a mensual
    Entonces el ciclo en curso sigue siendo del 1 al 15

  @listo
  Escenario: Un día fuera de rango se rechaza
    Cuando pongo 45 como día de inicio y guardo
    Entonces la respuesta es 422
    Y el error aparece junto al campo
```

---

## Cobertura automatizada hoy

| Prueba | Qué cubre |
|---|---|
| `OnboardingIntegrationTest` | El estado deducido de los datos, terminar con y sin ingresos, el doble terminar y «hacerlo después» |
| `AbandonedOnboardingJobTest` | La limpieza: a quién toca, a quién no, que invalida sin borrar y que se puede apagar |

Los escenarios de arriba siguen valiendo: cubren el contrato HTTP y la interfaz,
que las pruebas de Java no ejercitan.
