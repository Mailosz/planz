package pl.mo.planz.dto;

import java.util.List;

import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

public class TemplateDTO {
    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    String content;

    @Getter
    @Setter
    List<TemplateFieldDTO> fields;
}
