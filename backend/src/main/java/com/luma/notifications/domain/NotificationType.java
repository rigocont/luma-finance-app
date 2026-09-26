package com.luma.notifications.domain;

/** Las tres condiciones que LUMA vigila por su cuenta. */
public enum NotificationType {

    /** Un pago esta a pocos dias de vencer. */
    PAYMENT_DUE_SOON,

    /** Un pago paso su fecha sin ocurrir. */
    PAYMENT_OVERDUE,

    /** El ciclo en curso no alcanza con lo planeado. */
    CYCLE_DEFICIT
}
