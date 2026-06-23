package com.academconnect.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_pendiente")
@Getter
@Setter
public class MailPendiente extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String destinatario;

    @Column(nullable = false, length = 300)
    private String asunto;

    @Column(name = "cuerpo_html", nullable = false, columnDefinition = "text")
    private String cuerpoHtml;

    @Column(name = "cuerpo_texto", nullable = false, columnDefinition = "text")
    private String cuerpoTexto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMail estado = EstadoMail.PENDIENTE;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "enviado_en")
    private Instant enviadoEn;
}
