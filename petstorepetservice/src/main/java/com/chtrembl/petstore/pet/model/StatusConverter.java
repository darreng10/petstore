package com.chtrembl.petstore.pet.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Pet.Status, String> {

    @Override
    public String convertToDatabaseColumn(Pet.Status status) {
        if (status == null) {
            return null;
        }
        // Store lowercase in database
        return status.getValue();
    }

    @Override
    public Pet.Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Convert from lowercase database value to enum
        return Pet.Status.fromValue(dbData);
    }
}

