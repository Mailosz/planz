package pl.mo.planz.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;
import pl.mo.planz.model.DocumentModel;

public class DocumentDTO {

    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "dd-MM-yyyy")
    LocalDate week;

    @Getter
    @Setter
    UUID templateId;
}
