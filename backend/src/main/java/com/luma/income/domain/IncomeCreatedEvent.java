package com.luma.income.domain;

/**
 * Se capturo un ingreso nuevo.
 *
 * <p>Existe para resolver un problema de direccion, no de asincronia. El modulo
 * de presupuesto ya conoce al de ingresos: los materializa. Si el de ingresos
 * llamara de vuelta al de presupuesto para meter el ingreso nuevo al ciclo
 * abierto, los dos modulos quedarian apuntandose entre si, y esa es la primera
 * grieta por la que un monolito modular deja de ser modular.
 *
 * <p>Con el evento, ingresos solo anuncia lo que paso. Quien quiera reaccionar
 * —hoy el presupuesto, manana las notificaciones— lo hace sin que ingresos se
 * entere de su existencia.
 *
 * <p>Se publica y se atiende dentro de la MISMA transaccion: si materializar
 * falla, no queda un ingreso guardado a medias con un ciclo inconsistente.
 */
public record IncomeCreatedEvent(Income income) {}
