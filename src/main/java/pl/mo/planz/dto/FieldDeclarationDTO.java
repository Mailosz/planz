package pl.mo.planz.dto;

import lombok.Getter;
import lombok.Setter;
import pl.mo.planz.model.FieldType;

public class FieldDeclarationDTO {

    @Getter
    @Setter
    String value;

    @Getter
    @Setter
    String defaultValue;

    @Getter
    @Setter
    FieldType type;

    @Getter
    @Setter
    String name;
}
