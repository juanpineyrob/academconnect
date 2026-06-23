package com.academconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.TokenCuenta;

public interface TokenCuentaRepository extends JpaRepository<TokenCuenta, Long> {

    Optional<TokenCuenta> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM TokenCuenta t WHERE t.usuarioId = :usuarioId "
            + "AND t.proposito = :proposito AND t.usadoEn IS NULL")
    void deleteNoUsadosPorUsuarioYProposito(@Param("usuarioId") Long usuarioId,
                                            @Param("proposito") PropositoToken proposito);
}
