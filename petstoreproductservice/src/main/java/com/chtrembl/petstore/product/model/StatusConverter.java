package com.chtrembl.petstore.product.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Product.Status, String> {

    @Override
    public String convertToDatabaseColumn(Product.Status status) {
        if (status == null) {
            return null;
        }
        // Store lowercase in database
        return status.getValue();
    }

    @Override
    public Product.Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Convert from lowercase database value to enum
        return Product.Status.fromValue(dbData);
    }
}

