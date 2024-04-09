package pl.mo.planz.converters;

import java.time.Period;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PeriodToStringConverter implements AttributeConverter<Period, String> {
    @Override
    public String convertToDatabaseColumn(Period period) {
        return period != null ? period.toString() : null;
    }

    @Override
    public Period convertToEntityAttribute(String periodString) {
        return periodString != null ? Period.parse(periodString) : null;
    }
}