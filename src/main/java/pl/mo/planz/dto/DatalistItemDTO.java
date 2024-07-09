package pl.mo.planz.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DatalistItemDTO {
    
    private UUID id;
    private String name;
    private String description;
}
