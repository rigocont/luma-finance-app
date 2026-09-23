package com.luma.notifications.infrastructure;

import com.luma.config.LumaProperties;
import com.luma.notifications.application.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envio por SMTP.
 *
 * <p>En desarrollo apunta a Mailpit, que captura todo y lo muestra en una
 * bandeja web sin entregar nada a nadie. En produccion apunta al proveedor real.
 */
@Component
public class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

    private final JavaMailSender javaMailSender;
    private final LumaProperties properties;

    public SmtpMailer(JavaMailSender javaMailSender, LumaProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
    }

    @Override
    public boolean send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            javaMailSender.send(message);
            log.info("Correo enviado: asunto '{}'", subject);
            return true;
        } catch (RuntimeException e) {
            // Un servidor de correo caido no debe tumbar la peticion del usuario.
            log.error("No se pudo enviar el correo con asunto '{}'", subject, e);
            return false;
        }
    }
}
