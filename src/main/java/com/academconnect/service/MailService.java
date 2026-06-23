package com.academconnect.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.MailPendiente;
import com.academconnect.repository.MailPendienteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final MailPendienteRepository repo;
    private final MailDispatchWorker worker;

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

    /**
     * Oleada de envío: toma hasta {@code loteSize} correos PENDIENTE y delega cada uno a
     * {@link MailDispatchWorker#procesarUno(MailPendiente)}, que lo envía y persiste su estado en su
     * propia transacción ({@code REQUIRES_NEW}). Una excepción en un correo no aborta la oleada.
     *
     * <p>La entrega es <strong>at-least-once</strong>: si la aplicación cae entre el envío SMTP y el
     * commit del estado, el correo seguirá PENDIENTE y se reenviará en la próxima oleada. El drenador
     * asume además una <strong>única instancia de la aplicación</strong>: no hay claim ni bloqueo a
     * nivel de fila, por lo que dos instancias tomarían los mismos correos. Para multi-instancia,
     * reclamar las filas con {@code FOR UPDATE SKIP LOCKED} antes de enviarlas.
     */
    @Scheduled(fixedDelayString = "${academconnect.mail.drain-fixed-delay-ms:30000}")
    public void drenar() {
        List<MailPendiente> lote = repo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(loteSize));
        for (MailPendiente m : lote) {
            try {
                worker.procesarUno(m);
            } catch (Exception e) {
                log.warn("Error al procesar mail id={}: {}", m.getId(), e.getMessage());
            }
        }
    }
}
