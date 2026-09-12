package com.tmd.backend.domain.pet;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class PetBreedJavaType extends EnumJavaType<PetBreed> {
    public PetBreedJavaType() {
        super(PetBreed.class);
    }

    @Override
    public String getCheckCondition(
        String columnName,
        JdbcType jdbcType,
        BasicValueConverter<PetBreed, ?> converter,
        Dialect dialect
    ) {
        return null;
    }
}
