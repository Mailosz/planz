package pl.mo.planz.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pl.mo.planz.model.FieldType;

public class FieldDeclarationDTO {

    @Getter
    @Setter
    String value;

    @Getter
    @Setter
    String template;

    @Getter
    @Setter
    String defaultValue;

    @Getter
    @Setter
    FieldType type;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    String datalist;

    @Getter
    @Setter
    String editPermission;

    @Getter
    @Setter
    List<String> eligibleEditors;
}
