package com.academconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.academconnect.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MailTemplateServiceTests {

    @Autowired
    MailTemplateService templates;

    @Test
    void renderActivacionSustituyePlaceholders() {
        var m = templates.activacion("Ana", "abc123");
        assertThat(m.asunto()).isNotBlank();
        assertThat(m.html()).contains("Ana").contains("/establecer-password?token=abc123").doesNotContain("{{");
        assertThat(m.texto()).contains("Ana").contains("/establecer-password?token=abc123").doesNotContain("{{");
    }
}
