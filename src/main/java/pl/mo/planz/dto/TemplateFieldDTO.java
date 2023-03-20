package pl.mo.planz.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

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
    String auto;
    
    @Getter
    @Setter
    String list;

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
