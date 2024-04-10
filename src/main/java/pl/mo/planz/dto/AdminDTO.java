package pl.mo.planz.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {
    private String seriesName;
    private List<AccessDTO> accessList;
}
