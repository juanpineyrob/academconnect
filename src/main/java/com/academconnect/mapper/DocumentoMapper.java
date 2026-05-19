package com.academconnect.mapper;

import com.academconnect.domain.Documento;
import com.academconnect.dto.DocumentoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentoMapper {

    DocumentoResponse toResponse(Documento documento);
}
