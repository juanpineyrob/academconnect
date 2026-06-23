package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.academconnect.TestcontainersConfiguration;
import com.academconnect.domain.EstadoMail;
import com.academconnect.repository.MailPendienteRepository;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MailServiceTests {

    @RegisterExtension
    static GreenMailExtension green =
            new GreenMailExtension(ServerSetupTest.SMTP.port(3025)).withPerMethodLifecycle(true);

    @Autowired
    MailService mailService;

    @Autowired
    MailPendienteRepository repo;

    @Test
    @Transactional
    void encolarPersistePendiente() {
        mailService.encolar("a@x.test", "Asunto", "<p>hola</p>", "hola");
        assertThat(repo.findByEstadoOrderByCreatedAtAsc(EstadoMail.PENDIENTE, Pageable.ofSize(10))).hasSize(1);
    }

    @Test
    void drenarEnviaYMarcaEnviado() throws Exception {
        mailService.encolar("dest@x.test", "Asunto", "<p>hola</p>", "hola");
        mailService.drenar();
        green.waitForIncomingEmail(5000, 1);
        assertThat(green.getReceivedMessages()).hasSize(1);
        assertThat(green.getReceivedMessages()[0].getAllRecipients()[0].toString()).isEqualTo("dest@x.test");
    }
}
