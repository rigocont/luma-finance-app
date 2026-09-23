package com.luma.budget.domain;

/**
 * Que tanto margen hay para mover un pago.
 *
 * <p>Existe por una razon de seguridad del producto, no de comodidad: el modulo
 * de analisis debe poder distinguir la renta de una suscripcion de musica. Sin
 * este dato, una recomendacion automatica podria sugerir retrasar algo que no
 * se puede retrasar. La persona lo indica al crear el gasto.
 */
public enum Flexibility {

    /** No se mueve. Renta, hipoteca, colegiatura, pago minimo de una tarjeta. */
    CRITICAL,

    /** Se puede ajustar con consecuencias menores. */
    IMPORTANT,

    /** Se puede reducir o posponer sin mayor problema. */
    FLEXIBLE;

    /** Nunca se debe sugerir retrasar un pago critico. */
    public boolean canBeDeferred() {
        return this != CRITICAL;
    }
}
