package com.academconnect.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academconnect.domain.EstadoCuenta;
import com.academconnect.domain.Estudiante;
import com.academconnect.domain.PropositoToken;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.Usuario;
import com.academconnect.event.ActividadEvent;
import com.academconnect.repository.SolicitudCuentaRepository;
import com.academconnect.repository.UsuarioRepository;

/**
 * Test de servicio aislado: verifica que {@code establecerPassword} publica el evento
 * de actividad correcto. Se usa un publisher mockeado pasado por constructor (no
 * {@code @MockitoBean}) porque el {@code ApplicationEventPublisher} autowireado en el
 * contexto real es el propio {@code ApplicationContext} y no es reemplazable por un
 * mock-bean. No se afirma sobre la {@code Actividad} persistida (listener {@code @Async}).
 */
@ExtendWith(MockitoExtension.class)
class OnboardingServiceEventosTests {

    @Mock private TokenCuentaService tokenService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SolicitudCuentaRepository solicitudRepository;
    @Mock private MailService mailService;
    @Mock private MailTemplateService templates;
    @Mock private ApplicationEventPublisher eventos;

    private OnboardingService service() {
        return new OnboardingService(tokenService, usuarioRepository, passwordEncoder,
                solicitudRepository, mailService, templates, eventos);
    }

    private Usuario invitada(Long id, String matricula) {
        Usuario u = new Estudiante();
        u.setId(id);
        u.setMatricula(matricula);
        u.setEstadoCuenta(EstadoCuenta.INVITADA);
        u.setPassword(null);
        return u;
    }

    @Test
    void activacionPublicaCuentaActivada() {
        Usuario u = invitada(7L, "M-7");
        when(tokenService.propositoSiUsable("tok")).thenReturn(PropositoToken.ACTIVACION);
        when(tokenService.consumir("tok", PropositoToken.ACTIVACION)).thenReturn(u);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service().establecerPassword("tok", "NuevaPass123");

        verify(eventos).publishEvent(argThat((Object e) ->
                e instanceof ActividadEvent ev
                        && ev.tipo() == TipoActividad.CUENTA_ACTIVADA
                        && "USUARIO".equals(ev.recursoTipo())
                        && Long.valueOf(7L).equals(ev.recursoId())
                        && Long.valueOf(7L).equals(ev.actorId())
                        && "M-7".equals(ev.payload().get("matricula"))));
    }

    @Test
    void resetPublicaPasswordRestablecida() {
        Usuario u = invitada(9L, null);
        u.setEstadoCuenta(EstadoCuenta.ACTIVA);
        u.setPassword("viejo");
        when(tokenService.propositoSiUsable("tok")).thenReturn(PropositoToken.RESET);
        when(tokenService.consumir("tok", PropositoToken.RESET)).thenReturn(u);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service().establecerPassword("tok", "NuevaPass123");

        verify(eventos).publishEvent(argThat((Object e) ->
                e instanceof ActividadEvent ev
                        && ev.tipo() == TipoActividad.PASSWORD_RESTABLECIDA
                        && "USUARIO".equals(ev.recursoTipo())
                        && Long.valueOf(9L).equals(ev.recursoId())
                        && "".equals(ev.payload().get("matricula"))));
    }
}
