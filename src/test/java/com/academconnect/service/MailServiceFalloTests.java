package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.MailPendiente;
import com.academconnect.repository.MailPendienteRepository;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Verifica la ruta de fallo/reintento del drenador: tras {@code MAX_INTENTOS} oleadas fallidas el
 * correo queda FALLIDO, con {@code intentos} incrementándose y {@code ultimoError} capturado.
 * Usa un {@link JavaMailSender} mockeado cuyo {@code send} lanza siempre, de forma determinista.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MailServiceFalloTests {

    @Autowired
    MailService mailService;

    @Autowired
    MailPendienteRepository repo;

    @MockitoBean
    JavaMailSender mailSender;

    @Test
    void drenarMarcaFallidoTrasMaxIntentos() {
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        given(mailSender.createMimeMessage()).willReturn(mime);
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(MimeMessage.class));

        mailService.encolar("dest@x.test", "Asunto", "<p>hola</p>", "hola");

        Long id = pendienteUnico().getId();

        // Oleada 1: sigue PENDIENTE, intentos=1
        mailService.drenar();
        MailPendiente tras1 = repo.findById(id).orElseThrow();
        assertThat(tras1.getEstado()).isEqualTo(EstadoMail.PENDIENTE);
        assertThat(tras1.getIntentos()).isEqualTo(1);
        assertThat(tras1.getUltimoError()).contains("SMTP caído");

        // Oleada 2: sigue PENDIENTE, intentos=2
        mailService.drenar();
        MailPendiente tras2 = repo.findById(id).orElseThrow();
        assertThat(tras2.getEstado()).isEqualTo(EstadoMail.PENDIENTE);
        assertThat(tras2.getIntentos()).isEqualTo(2);

        // Oleada 3: ahora FALLIDO, intentos=3
        mailService.drenar();
        MailPendiente tras3 = repo.findById(id).orElseThrow();
        assertThat(tras3.getEstado()).isEqualTo(EstadoMail.FALLIDO);
        assertThat(tras3.getIntentos()).isEqualTo(3);
        assertThat(tras3.getUltimoError()).isNotNull();
    }

    private MailPendiente pendienteUnico() {
        List<MailPendiente> pendientes =
                repo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10));
        assertThat(pendientes).hasSize(1);
        return pendientes.get(0);
    }
}
