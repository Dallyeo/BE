package com.ppip.dallyeo.course.converter;

import com.ppip.dallyeo.course.dto.WaypointAnchor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * List&lt;WaypointAnchor&gt; ↔ JSON TEXT 컬럼 (D1).
 */
@Converter
public class WaypointAnchorsConverter implements AttributeConverter<List<WaypointAnchor>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<WaypointAnchor>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<WaypointAnchor> attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute);
    }

    @Override
    public List<WaypointAnchor> convertToEntityAttribute(String dbData) {
        return (dbData == null || dbData.isBlank()) ? List.of() : MAPPER.readValue(dbData, TYPE);
    }
}
