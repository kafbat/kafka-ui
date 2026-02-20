package io.kafbat.ui.mapper;

import io.kafbat.ui.model.CompatibilityCheckResponseDTO;
import io.kafbat.ui.model.CompatibilityLevelDTO;
import io.kafbat.ui.model.NewSchemaSubjectDTO;
import io.kafbat.ui.model.SchemaReferenceDTO;
import io.kafbat.ui.model.SchemaSubjectDTO;
import io.kafbat.ui.model.SchemaTypeDTO;
import io.kafbat.ui.service.SchemaRegistryService;
import io.kafbat.ui.sr.model.Compatibility;
import io.kafbat.ui.sr.model.CompatibilityCheckResponse;
import io.kafbat.ui.sr.model.NewSubject;
import io.kafbat.ui.sr.model.SchemaReference;
import io.kafbat.ui.sr.model.SchemaType;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;


@Mapper
public interface KafkaSrMapper {

  default SchemaSubjectDTO toDto(SchemaRegistryService.SubjectWithCompatibilityLevel s) {
    return new SchemaSubjectDTO()
        .id(s.getId())
        .version(s.getVersion())
        .subject(s.getSubject())
        .schema(s.getSchema())
        .schemaType(SchemaTypeDTO.fromValue(Optional.ofNullable(s.getSchemaType()).orElse(SchemaType.AVRO).getValue()))
        .references(toDto(s.getReferences()))
        .topic(s.getTopic())
        .compatibilityLevel(s.getCompatibility().toString());
  }

  List<SchemaReferenceDTO> toDto(List<SchemaReference> references);

  CompatibilityCheckResponseDTO toDto(CompatibilityCheckResponse ccr);

  CompatibilityLevelDTO.CompatibilityEnum toDto(Compatibility compatibility);

  NewSubject fromDto(NewSchemaSubjectDTO subjectDto);

  Compatibility fromDto(CompatibilityLevelDTO.CompatibilityEnum dtoEnum);
}
