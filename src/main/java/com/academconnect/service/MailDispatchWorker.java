package com.academconnect.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.MailPendiente;
import com.academconnect.repository.MailPendienteRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envía un único correo dentro de su propia transacción.
 *
 * <p>Vive en un componente separado (en lugar de un método de {@link MailService}) porque
 * {@link Propagation#REQUIRES_NEW} sólo se aplica cuando la invocación pasa por el proxy de Spring;
 * una llamada interna {@code this.procesarUno(...)} omitiría el proxy y reutilizaría la transacción
 * de la oleada, anulando el aislamiento por correo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailDispatchWorker {

    private static final int MAX_INTENTOS = 3;

    private final MailPendienteRepository repo;
    private final JavaMailSender mailSender;

    @Value("${academconnect.mail.from}")
    private String from;

    /**
     * Envía un correo y persiste su nuevo estado en una transacción independiente.
     *
     * <p>Recibe la entidad (posiblemente desligada) leída por la oleada y la guarda con su estado
     * mutado. Si el envío SMTP tiene éxito pero el commit falla, el correo podría reenviarse en la
     * próxima oleada: la entrega es <em>at-least-once</em> (ver {@link MailService#drenar()}).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarUno(MailPendiente m) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mime, true, "UTF-8");
            h.setFrom(from);
            h.setTo(m.getDestinatario());
            h.setSubject(m.getAsunto());
            h.setText(m.getCuerpoTexto(), m.getCuerpoHtml());
            mailSender.send(mime);
            m.setEstado(EstadoMail.ENVIADO);
            m.setEnviadoEn(Instant.now());
        } catch (Exception e) {
            m.setIntentos(m.getIntentos() + 1);
            m.setUltimoError(e.getMessage() == null
                    ? "error"
                    : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
            if (m.getIntentos() >= MAX_INTENTOS) {
                m.setEstado(EstadoMail.FALLIDO);
                log.warn("Mail a {} falló definitivamente: {}", m.getDestinatario(), m.getUltimoError());
            }
        }
        repo.save(m);
    }
}
