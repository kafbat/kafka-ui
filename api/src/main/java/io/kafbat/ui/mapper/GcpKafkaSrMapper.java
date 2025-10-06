package io.kafbat.ui.mapper;

import io.kafbat.ui.model.CompatibilityCheckResponseDTO;
import io.kafbat.ui.model.CompatibilityLevelDTO;
import io.kafbat.ui.model.NewSchemaSubjectDTO;
import io.kafbat.ui.model.SchemaReferenceDTO;
import io.kafbat.ui.model.SchemaSubjectDTO;
import io.kafbat.ui.model.SchemaTypeDTO;
import io.kafbat.ui.service.GcpSchemaRegistryService;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;

@Mapper
public interface GcpKafkaSrMapper {

  // Convert GCP SubjectWithCompatibilityLevel to DTO
  default SchemaSubjectDTO toDto(GcpSchemaRegistryService.SubjectWithCompatibilityLevel s) {
    return new SchemaSubjectDTO()
        .id(s.getId())
        .version(s.getVersion())
        .subject(s.getSubject())
        .schema(s.getSchema())
        .schemaType(SchemaTypeDTO.fromValue(
            Optional.ofNullable(s.getSchemaType())
                .orElse(io.kafbat.ui.gcp.sr.model.SchemaType.AVRO)
                .getValue()))
        .references(toDto(s.getReferences()))
        .compatibilityLevel(Optional.ofNullable(s.getCompatibility())
            .map(Object::toString).orElse(null));
  }

  // Convert GCP SchemaReference list to DTO list
  List<SchemaReferenceDTO> toDto(List<io.kafbat.ui.gcp.sr.model.SchemaReference> references);

  // Convert GCP CompatibilityCheckResponse to DTO
  CompatibilityCheckResponseDTO toDto(io.kafbat.ui.gcp.sr.model.CompatibilityCheckResponse ccr);

  // Convert GCP Compatibility to DTO enum
  CompatibilityLevelDTO.CompatibilityEnum toDto(io.kafbat.ui.gcp.sr.model.Compatibility compatibility);

  // Convert DTO to GCP NewSubject
  io.kafbat.ui.gcp.sr.model.NewSubject fromDto(NewSchemaSubjectDTO subjectDto);

  // Convert DTO enum to GCP Compatibility
  io.kafbat.ui.gcp.sr.model.Compatibility fromDto(CompatibilityLevelDTO.CompatibilityEnum dtoEnum);

}
