package com.luma.common.error;

/**
 * Credenciales invalidas, o cuenta que no puede iniciar sesion.
 *
 * <p>El mensaje es deliberadamente vago: decir "ese correo no existe" permite
 * averiguar quien tiene cuenta en LUMA probando correos.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Correo o contrasena incorrectos.");
    }
}
