package com.academconnect.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.MailPendiente;
import com.academconnect.repository.MailPendienteRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final int MAX_INTENTOS = 3;

    private final MailPendienteRepository repo;
    private final JavaMailSender mailSender;

    @Value("${academconnect.mail.from}")
    private String from;

    @Value("${academconnect.mail.lote-size:25}")
    private int loteSize;

    @Transactional
    public void encolar(String destinatario, String asunto, String html, String texto) {
        MailPendiente m = new MailPendiente();
        m.setDestinatario(destinatario);
        m.setAsunto(asunto);
        m.setCuerpoHtml(html);
        m.setCuerpoTexto(texto);
        repo.save(m);
    }

    /** Oleada: toma hasta loteSize pendientes y los envía. Disparado por @Scheduled o manualmente (admin/import). */
    @Scheduled(fixedDelayString = "${academconnect.mail.drain-fixed-delay-ms:30000}")
    @Transactional
    public void drenar() {
        List<MailPendiente> lote = repo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(loteSize));
        for (MailPendiente m : lote) {
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
}
