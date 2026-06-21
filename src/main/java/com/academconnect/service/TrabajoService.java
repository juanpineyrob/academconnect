package com.academconnect.service;

import com.academconnect.domain.Administrador;
import com.academconnect.domain.AreaTematica;
import com.academconnect.domain.EstadoSolicitud;
import com.academconnect.domain.EstadoTrabajo;
import com.academconnect.domain.TipoActividad;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.Trabajo;
import com.academconnect.domain.Usuario;
import com.academconnect.domain.VisibilidadActividad;
import com.academconnect.dto.TrabajoAdminImportRequest;
import com.academconnect.dto.TrabajoEstudianteRequest;
import com.academconnect.dto.TrabajoRequest;
import com.academconnect.dto.TrabajoResponse;
import com.academconnect.event.ActividadEvent;
import com.academconnect.exception.BusinessException;
import com.academconnect.exception.ResourceNotFoundException;
import com.academconnect.mapper.TrabajoMapper;
import com.academconnect.dto.PublicarTrabajoRequest;
import com.academconnect.repository.AreaTematicaRepository;
import com.academconnect.repository.AsignacionRepository;
import com.academconnect.repository.EstudianteRepository;
import com.academconnect.repository.ProfesorRepository;
import com.academconnect.repository.SolicitudVinculacionRepository;
import com.academconnect.repository.TrabajoRepository;
import com.academconnect.repository.UsuarioRepository;
import com.academconnect.repository.spec.TrabajoSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TrabajoService {

    private final TrabajoRepository trabajoRepository;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaTematicaRepository areaTematicaRepository;
    private final AsignacionRepository asignacionRepository;
    private final SolicitudVinculacionRepository solicitudRepository;
    private final TrabajoMapper mapper;
    private final ApplicationEventPublisher events;

    @Value("${academconnect.storage.trabajos:./data/trabajos}")
    private String trabajosRoot;

    public List<TrabajoResponse> listar() {
        return trabajoRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<TrabajoResponse> listarPorEstado(EstadoTrabajo estado) {
        return trabajoRepository.findByEstado(estado).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public TrabajoResponse buscarPorId(Long id) {
        return trabajoRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
    }

    @Transactional
    public TrabajoResponse crear(TrabajoRequest request) {
        var orientador = profesorRepository.findById(request.orientadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", request.orientadorId()));

        var trabajo = mapper.toEntity(request);
        trabajo.setOrientador(orientador);
        trabajo.setEstado(EstadoTrabajo.BORRADOR);
        trabajo.setKeywords(normalizarKeywords(request.keywords()));

        if (request.areaIds() != null && !request.areaIds().isEmpty()) {
            Set<AreaTematica> areas = new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()));
            trabajo.setAreas(areas);
        }

        Trabajo saved = trabajoRepository.save(trabajo);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.TRABAJO_CREADO,
                orientador.getId(),
                "TRABAJO", saved.getId(),
                Map.of("titulo", saved.getTitulo(), "tipo", saved.getTipo().name()),
                VisibilidadActividad.PARTICIPANTES,
                participantesDe(saved)));
        return mapper.toResponse(saved);
    }

    /** Camino 2.1 — el estudiante crea su propio trabajo en BORRADOR sin orientador. */
    @Transactional
    public TrabajoResponse crearPorEstudiante(TrabajoEstudianteRequest request, Long estudianteId) {
        var estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", estudianteId));

        var trabajo = new Trabajo();
        trabajo.setTitulo(request.titulo());
        trabajo.setDescripcion(request.descripcion());
        trabajo.setTipo(request.tipo());
        trabajo.setEstado(EstadoTrabajo.BORRADOR);
        trabajo.setEstudiante(estudiante);
        trabajo.setKeywords(normalizarKeywords(request.keywords()));

        if (request.areaIds() != null && !request.areaIds().isEmpty()) {
            Set<AreaTematica> areas = new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()));
            trabajo.setAreas(areas);
        }

        Trabajo saved = trabajoRepository.save(trabajo);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.TRABAJO_CREADO,
                estudiante.getId(),
                "TRABAJO", saved.getId(),
                Map.of("titulo", saved.getTitulo(), "tipo", saved.getTipo().name(), "origen", "ESTUDIANTE"),
                VisibilidadActividad.PARTICIPANTES,
                participantesDe(saved)));
        return mapper.toResponse(saved);
    }

    /** Camino 2.1 — el estudiante actualiza su propio trabajo si está en BORRADOR. */
    @Transactional
    public TrabajoResponse actualizarBorradorPorEstudiante(Long trabajoId, TrabajoEstudianteRequest request, Long estudianteId) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));
        if (trabajo.getEstudiante() == null || !trabajo.getEstudiante().getId().equals(estudianteId)) {
            throw new BusinessException("No sos el dueño de este trabajo");
        }
        if (trabajo.getEstado() != EstadoTrabajo.BORRADOR) {
            throw new BusinessException("Solo se pueden editar trabajos en estado BORRADOR");
        }

        trabajo.setTitulo(request.titulo());
        trabajo.setDescripcion(request.descripcion());
        trabajo.setTipo(request.tipo());
        trabajo.setKeywords(normalizarKeywords(request.keywords()));

        Set<AreaTematica> areas = (request.areaIds() != null && !request.areaIds().isEmpty())
                ? new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()))
                : new HashSet<>();
        trabajo.setAreas(areas);

        return mapper.toResponse(trabajoRepository.save(trabajo));
    }

    /** Camino 2.1 — lista todos los trabajos del estudiante. */
    public List<TrabajoResponse> listarMisBorradores(Long estudianteId) {
        return trabajoRepository.findByEstudianteId(estudianteId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Importación legacy: el administrador da de alta trabajos finalizados fuera del sistema.
     * No pasa por el state machine — el {@code estado} es el del request (típicamente APROBADO).
     */
    @Transactional
    public TrabajoResponse importarLegacy(TrabajoAdminImportRequest request) {
        var orientador = profesorRepository.findById(request.orientadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", request.orientadorId()));

        var trabajo = new Trabajo();
        trabajo.setTitulo(request.titulo());
        trabajo.setDescripcion(request.descripcion());
        trabajo.setTipo(request.tipo());
        trabajo.setEstado(request.estado());
        trabajo.setOrientador(orientador);
        trabajo.setKeywords(normalizarKeywords(request.keywords()));
        trabajo.setPuntajeAgregado(request.puntajeAgregado());
        trabajo.setEvaluadoEn(request.evaluadoEn());
        trabajo.setArchivoStorageKey(request.archivoStorageKey());

        if (request.estudianteId() != null) {
            var estudiante = estudianteRepository.findById(request.estudianteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estudiante", request.estudianteId()));
            trabajo.setEstudiante(estudiante);
        }

        if (request.areaIds() != null && !request.areaIds().isEmpty()) {
            Set<AreaTematica> areas = new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()));
            trabajo.setAreas(areas);
        }

        Trabajo saved = trabajoRepository.save(trabajo);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.TRABAJO_CREADO,
                null,
                "TRABAJO", saved.getId(),
                Map.of("titulo", saved.getTitulo(), "tipo", saved.getTipo().name(), "origen", "ADMIN_IMPORT"),
                VisibilidadActividad.PUBLICA,
                participantesDe(saved)));
        return mapper.toResponse(saved);
    }

    private static List<Long> participantesDe(Trabajo t) {
        List<Long> ids = new java.util.ArrayList<>();
        if (t.getOrientador() != null) ids.add(t.getOrientador().getId());
        if (t.getEstudiante() != null) ids.add(t.getEstudiante().getId());
        return ids;
    }

    public List<TrabajoResponse> buscarPorTexto(String q) {
        return trabajoRepository.buscarPorTexto(q).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * G12+G13 — búsqueda multi-parámetro combinable. Cuando {@code soloPublicos=true}, fuerza
     * {@code estado=APROBADO} e ignora filtros que expongan datos privados (orientador, estudiante).
     * El controlador define {@code soloPublicos} según haya o no autenticación.
     */
    public Page<TrabajoResponse> buscar(
            String q,
            List<Long> areaIds,
            List<Integer> anios,
            TipoTrabajo tipo,
            EstadoTrabajo estado,
            Long orientadorId,
            Long estudianteId,
            boolean soloPublicos,
            boolean incluirOcultos,
            Pageable pageable) {

        // Specification que matchea todo; los predicados se acumulan con and(...).
        Specification<Trabajo> spec = (root, cq, cb) -> cb.conjunction();

        if (soloPublicos) {
            spec = spec.and(TrabajoSpecs.estadoIgual(EstadoTrabajo.APROBADO));
        } else if (estado != null) {
            spec = spec.and(TrabajoSpecs.estadoIgual(estado));
        } else if (orientadorId == null && estudianteId == null) {
            // Repositorio: sin filtro explícito de estado ni de participante, solo trabajos APROBADOS.
            spec = spec.and(TrabajoSpecs.estadoIgual(EstadoTrabajo.APROBADO));
        }

        // Los trabajos ocultados por moderación solo son visibles para administradores.
        if (!incluirOcultos) {
            spec = spec.and(TrabajoSpecs.noOculto());
        }

        if (q != null && !q.isBlank()) {
            List<Long> ids = trabajoRepository.buscarIdsPorTexto(q);
            spec = spec.and(TrabajoSpecs.idEnConjunto(ids));
        }

        spec = combinar(spec, TrabajoSpecs.tieneAlgunArea(areaIds));
        spec = combinar(spec, TrabajoSpecs.creadoEnAnio(anios));
        spec = combinar(spec, TrabajoSpecs.tipoIgual(tipo));
        spec = combinar(spec, TrabajoSpecs.estudianteIgual(estudianteId));
        if (!soloPublicos) {
            spec = combinar(spec, TrabajoSpecs.orientadorIgual(orientadorId));
        }

        return trabajoRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    private static Specification<Trabajo> combinar(Specification<Trabajo> base, Specification<Trabajo> extra) {
        return extra == null ? base : base.and(extra);
    }

    public List<TrabajoResponse> listarMisTrabajos(String estudianteEmail) {
        var usuario = usuarioRepository.findByEmail(estudianteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con email", estudianteEmail));
        return trabajoRepository.findByEstudianteId(usuario.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public TrabajoResponse aprobar(Long id) {
        var r = cambiarEstado(id, EstadoTrabajo.EN_EVALUACION, EstadoTrabajo.APROBADO);
        publicarTransicion(id, TipoActividad.TRABAJO_APROBADO);
        return r;
    }

    @Transactional
    public TrabajoResponse rechazar(Long id) {
        var r = cambiarEstado(id, EstadoTrabajo.EN_EVALUACION, EstadoTrabajo.RECHAZADO);
        publicarTransicion(id, TipoActividad.TRABAJO_RECHAZADO);
        return r;
    }

    private void publicarTransicion(Long trabajoId, TipoActividad tipo) {
        trabajoRepository.findById(trabajoId).ifPresent(t ->
                events.publishEvent(ActividadEvent.of(
                        tipo, null, "TRABAJO", t.getId(),
                        Map.of("titulo", t.getTitulo()),
                        VisibilidadActividad.PUBLICA,
                        participantesDe(t))));
    }

    /**
     * Override de administrador (recuperación ante fallos): elimina físicamente un trabajo
     * y todo su árbol. Las asignaciones tienen FK ON DELETE RESTRICT, así que se borran antes
     * (sus evaluaciones y calificaciones caen por ON DELETE CASCADE); el resto de los hijos
     * —versionamientos, solicitudes, invitaciones, sesiones, coorientadores, áreas,
     * recomendaciones, conflictos— cae por ON DELETE CASCADE al borrar el trabajo.
     */
    @Transactional
    public void eliminar(Long id) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
        var asignaciones = asignacionRepository.findByTrabajoId(id);
        if (!asignaciones.isEmpty()) {
            asignacionRepository.deleteAllInBatch(asignaciones);
        }
        trabajoRepository.delete(trabajo);
    }

    /** Override de administrador: oculta el trabajo del repositorio público (reversible, preserva el estado). */
    @Transactional
    public TrabajoResponse ocultar(Long id) {
        return setOculto(id, true);
    }

    /** Override de administrador: vuelve a mostrar un trabajo previamente ocultado. */
    @Transactional
    public TrabajoResponse mostrar(Long id) {
        return setOculto(id, false);
    }

    private TrabajoResponse setOculto(Long id, boolean oculto) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
        trabajo.setOculto(oculto);
        return mapper.toResponse(trabajoRepository.save(trabajo));
    }

    @Transactional
    public TrabajoResponse actualizar(Long id, TrabajoRequest request) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));

        var orientador = profesorRepository.findById(request.orientadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor", request.orientadorId()));

        mapper.update(request, trabajo);
        trabajo.setOrientador(orientador);
        trabajo.setKeywords(normalizarKeywords(request.keywords()));

        Set<AreaTematica> areas = (request.areaIds() != null && !request.areaIds().isEmpty())
                ? new HashSet<>(areaTematicaRepository.findAllById(request.areaIds()))
                : new HashSet<>();
        trabajo.setAreas(areas);

        return mapper.toResponse(trabajoRepository.save(trabajo));
    }

    /** Lowercase + trim + dedupe preservando orden. La cardinalidad la garantiza el @Size del DTO + CHECK SQL. */
    private static java.util.List<String> normalizarKeywords(java.util.List<String> keywords) {
        return keywords.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    private static final Set<Integer> DURACIONES_PERMITIDAS = Set.of(7, 15, 30, 60);

    /** Camino 2.2 — publica un BORRADOR como ABIERTO con expiración configurable. */
    @Transactional
    public TrabajoResponse publicar(Long trabajoId, PublicarTrabajoRequest request, Long profesorId) {
        if (!DURACIONES_PERMITIDAS.contains(request.duracionDias())) {
            throw new BusinessException("Duración inválida; debe ser 7, 15, 30 o 60 días");
        }
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));
        if (trabajo.getOrientador() == null || !trabajo.getOrientador().getId().equals(profesorId)) {
            throw new BusinessException("Solo el profesor orientador puede publicar el trabajo");
        }
        if (trabajo.getEstado() != EstadoTrabajo.BORRADOR) {
            throw new BusinessException("Solo se puede publicar desde BORRADOR");
        }
        if (trabajo.getAreas() == null || trabajo.getAreas().isEmpty()) {
            throw new BusinessException("Asigná al menos un área antes de publicar");
        }
        if (trabajo.getKeywords() == null || trabajo.getKeywords().size() < 3) {
            throw new BusinessException("Asigná entre 3 y 8 keywords antes de publicar");
        }

        trabajo.setEstado(EstadoTrabajo.ABIERTO);
        trabajo.setExpiraEn(java.time.Instant.now().plus(request.duracionDias(), java.time.temporal.ChronoUnit.DAYS));

        Trabajo saved = trabajoRepository.save(trabajo);
        events.publishEvent(ActividadEvent.of(
                TipoActividad.TRABAJO_PUBLICADO,
                profesorId,
                "TRABAJO", saved.getId(),
                Map.of("titulo", saved.getTitulo(), "duracionDias", request.duracionDias()),
                VisibilidadActividad.PUBLICA,
                participantesDe(saved)));
        return mapper.toResponse(saved);
    }

    /** Side-effect compartido: marca RECHAZADA todas las pendientes de un trabajo con respuesta dada. */
    @Transactional
    public void autoRechazarPendientes(Trabajo trabajo, String respuesta) {
        var pendientes = solicitudRepository.findByTrabajoIdAndEstado(trabajo.getId(), EstadoSolicitud.PENDIENTE);
        java.time.Instant now = java.time.Instant.now();
        for (var s : pendientes) {
            s.setEstado(EstadoSolicitud.RECHAZADA);
            s.setRespuesta(respuesta);
            s.setResueltaEn(now);
        }
        solicitudRepository.saveAll(pendientes);
    }

    /** Camino 2.2 — el profesor retira manualmente una publicación ABIERTA. */
    @Transactional
    public TrabajoResponse cerrar(Long trabajoId, Long profesorId) {
        var trabajo = trabajoRepository.findById(trabajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", trabajoId));
        if (trabajo.getOrientador() == null || !trabajo.getOrientador().getId().equals(profesorId)) {
            throw new BusinessException("Solo el profesor orientador puede cerrar el trabajo");
        }
        if (trabajo.getEstado() != EstadoTrabajo.ABIERTO) {
            throw new BusinessException("Solo se puede cerrar un trabajo ABIERTO");
        }

        trabajo.setEstado(EstadoTrabajo.CANCELADO);
        autoRechazarPendientes(trabajo, "Trabajo cerrado");
        Trabajo saved = trabajoRepository.save(trabajo);

        events.publishEvent(ActividadEvent.of(
                TipoActividad.TRABAJO_CERRADO,
                profesorId,
                "TRABAJO", saved.getId(),
                Map.of("titulo", saved.getTitulo()),
                VisibilidadActividad.PUBLICA,
                participantesDe(saved)));
        return mapper.toResponse(saved);
    }

    private TrabajoResponse cambiarEstado(Long id, EstadoTrabajo estadoRequerido, EstadoTrabajo nuevoEstado) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
        if (trabajo.getEstado() != estadoRequerido) {
            throw new BusinessException(
                    "El trabajo debe estar en estado %s para pasar a %s".formatted(estadoRequerido, nuevoEstado));
        }
        trabajo.setEstado(nuevoEstado);
        return mapper.toResponse(trabajoRepository.save(trabajo));
    }

    /**
     * Sirve el PDF asociado a un trabajo. Reglas de acceso:
     *  - APROBADO: público (auth opcional).
     *  - Otros estados: requiere autenticación y que el caller sea orientador, estudiante o administrador.
     */
    public ArchivoTrabajo descargarArchivo(Long id, Authentication auth) {
        var trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo", id));
        String storageKey = trabajo.getArchivoStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw new ResourceNotFoundException("Archivo del trabajo", id);
        }

        boolean esPublico = trabajo.getEstado() == EstadoTrabajo.APROBADO;
        if (!esPublico) {
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                throw new AccessDeniedException("Trabajo no disponible");
            }
            var usuario = usuarioRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
            if (!puedeAccederPDF(usuario, trabajo)) {
                throw new AccessDeniedException("Sin permisos sobre este trabajo");
            }
        }

        Path root = Path.of(trabajosRoot).toAbsolutePath().normalize();
        Path filepath = root.resolve(storageKey).normalize();
        if (!filepath.startsWith(root) || !Files.exists(filepath)) {
            throw new ResourceNotFoundException("Archivo del trabajo", id);
        }
        return new ArchivoTrabajo(new FileSystemResource(filepath), storageKey);
    }

    private boolean puedeAccederPDF(Usuario usuario, Trabajo trabajo) {
        if (usuario instanceof Administrador) return true;
        if (trabajo.getOrientador() != null && trabajo.getOrientador().getId().equals(usuario.getId())) return true;
        if (trabajo.getEstudiante() != null && trabajo.getEstudiante().getId().equals(usuario.getId())) return true;
        return false;
    }

    public record ArchivoTrabajo(Resource resource, String filename) {}
}
