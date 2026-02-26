package com.fungame.songquiz.storage.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        return String.join(DELIMITER, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String data) {
        if (data == null) {
            return List.of();
        }

        return Arrays.asList(data.split(DELIMITER));
    }
}
