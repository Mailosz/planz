package pl.mo.planz.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pl.mo.planz.model.FieldType;

public class TemplateFieldDTO {
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    String edit;
    
    @Getter
    @Setter
    String list;

    @Getter
    @Setter
    FieldType type;

    @Getter
    @Setter
    int pos;

    @Getter
    @Setter
    String defaultValue;

    @Getter
    @Setter
    Boolean isPublic;
}
