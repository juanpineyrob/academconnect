package com.academconnect.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.academconnect.domain.EstadoMail;
import com.academconnect.domain.MailPendiente;

public interface MailPendienteRepository extends JpaRepository<MailPendiente, Long> {
    List<MailPendiente> findByEstadoOrderByCreatedAtAsc(EstadoMail estado, Pageable pageable);
}
