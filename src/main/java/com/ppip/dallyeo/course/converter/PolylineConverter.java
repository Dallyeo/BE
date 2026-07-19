package com.ppip.dallyeo.course.converter;

import com.ppip.dallyeo.course.dto.PolylinePoint;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * List&lt;PolylinePoint&gt; ↔ JSON TEXT 컬럼 (D1). Jackson3 ObjectMapper 사용.
 * Hibernate가 인스턴스화하므로 ObjectMapper는 정적 공유.
 */
@Converter
public class PolylineConverter implements AttributeConverter<List<PolylinePoint>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<PolylinePoint>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<PolylinePoint> attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute);
    }

    @Override
    public List<PolylinePoint> convertToEntityAttribute(String dbData) {
        return (dbData == null || dbData.isBlank()) ? List.of() : MAPPER.readValue(dbData, TYPE);
    }
}
