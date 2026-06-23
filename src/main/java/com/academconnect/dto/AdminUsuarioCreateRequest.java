package com.academconnect.dto;

import com.academconnect.domain.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de usuario por administrador. Los campos de subtipo (titulacion/cargo para PROFESOR,
 * institucion/titulo para EXTERNO) se validan en el servicio según el rol.
 */
public record AdminUsuarioCreateRequest(
        @NotNull Rol rol,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 30) String matricula,
        @NotBlank @Size(max = 200) String nombre,
        Integer edad,
        @Size(max = 200) String ubicacion,
        @Size(max = 200) String titulacion,
        @Size(max = 200) String cargo,
        @Size(max = 200) String institucion,
        @Size(max = 200) String titulo) {
}
