package com.ppip.dallyeo.course.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * List&lt;Integer&gt; ↔ JSON TEXT 컬럼 (cumulativeMeters, D1).
 */
@Converter
public class IntListConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Integer>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute);
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        return (dbData == null || dbData.isBlank()) ? List.of() : MAPPER.readValue(dbData, TYPE);
    }
}
