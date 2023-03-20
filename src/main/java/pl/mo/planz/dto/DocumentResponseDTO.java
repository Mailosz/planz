package pl.mo.planz.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

public class DocumentResponseDTO {

    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    LocalDate week;

    @Getter
    @Setter
    UUID templateId;

    @Getter
    @Setter
    String templateContent;

    @Getter
    @Setter
    boolean hasPrev;

    @Getter
    @Setter
    boolean hasNext;

    @Getter
    @Setter
    Map<UUID, String> fieldValues;

    @Getter
    @Setter
    Map<String, List<String>> datalists;

}
