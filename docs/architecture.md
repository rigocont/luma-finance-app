# Arquitectura

El analisis completo, con modelo de datos, decisiones, riesgos y preguntas
resueltas, esta en [`00-arquitectura-fase-0.md`](00-arquitectura-fase-0.md).

Este documento es el resumen operativo.

---

## Forma general

```
  React SPA  ->  Spring Boot (monolito modular)  ->  MySQL 8.4
```

Un solo despliegue. Los modulos estan separados por dominio de negocio, no por
capa tecnica.

## Modulos del backend

```
com.luma
  common          Money, errores, paginacion, correlation id
  config          Seguridad, CORS, OpenAPI, propiedades
  auth            Registro, login, tokens                   (Fase 2)
  users           Perfil y preferencias                     (Fase 2)
  budget          Ciclos presupuestales y motor de balance  (Fase 4)
  income          Ingresos                                  (Fase 5)
  expenses        Gastos fijos y variables                  (Fases 6-7)
  savings         Metas y aportaciones                      (Fase 8)
  onboarding      Wizard inicial                            (Fase 9)
  dashboard       Composicion de resumenes                  (Fase 10)
  notifications   Alertas internas                          (Fase 11)
  subscriptions   Planes y acceso a funciones               (Fase 12)
  insights        Reglas deterministas y, despues, IA       (Fase 13)
  system          Diagnostico
```

## Capas dentro de cada modulo

```
api             Controladores REST y DTOs
application     Casos de uso, orquestacion, transacciones
domain          Entidades, value objects, reglas puras, puertos
infrastructure  JPA, adaptadores, clientes externos
```

**Direccion de las dependencias:**

```
api -> application -> domain <- infrastructure
```

El dominio no importa Spring ni JPA. `ArchitectureTest` lo verifica en cada build.

## Comunicacion entre modulos

A traves de interfaces publicas de la capa `application`. Un modulo nunca accede
al repositorio de otro.

Para efectos secundarios (por ejemplo: se cerro un ciclo, hay que generar una
notificacion) se usan eventos de Spring. Si algun dia hiciera falta un broker,
el cambio queda contenido.

## El motor presupuestal

`LUMA-BUDGET-ENGINE` es el corazon del producto y se construye como codigo de
dominio puro, sin dependencias de framework:

```java
public final class BudgetCalculator {
    public BudgetResult calculate(BudgetCycleSnapshot snapshot) { ... }
}
```

Entrada inmutable, salida inmutable, cero acceso a base de datos. Se prueba con
tests unitarios que corren en milisegundos.

Esto tambien es lo que consume el modulo de insights: **la IA nunca calcula
cifras**, recibe un `BudgetResult` ya calculado y solo lo interpreta. En una
aplicacion de dinero, un modelo de lenguaje no debe producir numeros.

## Decisiones que no se negocian

| Decision | Por que |
|---|---|
| La logica financiera vive en el backend | La app movil futura consume la misma API sin reimplementar nada |
| El esquema lo gobierna Flyway | La base se reconstruye desde cero; `ddl-auto` nunca pasa de `validate` |
| Los ciclos cerrados son inmutables | Si el dashboard calculara en vivo, subir la renta hoy cambiaria los meses pasados |
| El dinero es `BigDecimal` | El punto flotante produce cifras que no cuadran |
| El `userId` sale del token | Elimina por construccion el acceso a datos de otro usuario |
| DTOs separados de entidades | El contrato de la API no cambia porque cambie el esquema |
