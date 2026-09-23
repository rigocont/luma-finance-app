package com.luma.notifications.application;

/**
 * Puerto de salida para enviar correo.
 *
 * <p>El resto de la aplicacion depende de esta interfaz, no de un proveedor.
 * Cambiar de SMTP a Resend, SES o cualquier otro servicio significa escribir un
 * adaptador nuevo, sin tocar ningun caso de uso.
 */
public interface Mailer {

    /**
     * Envia un correo de texto plano.
     *
     * <p>No lanza excepciones: un fallo de envio se registra y se descarta. Quien
     * llama no debe fallar porque el servidor de correo este caido, y menos aun
     * dejar que eso se note en la respuesta de la API.
     *
     * @return true si el correo salio
     */
    boolean send(String to, String subject, String body);
}
