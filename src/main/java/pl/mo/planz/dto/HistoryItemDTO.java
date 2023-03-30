package pl.mo.planz.dto;

import lombok.Getter;
import lombok.Setter;

public class HistoryItemDTO {

    @Getter
    @Setter
    String value;


    @Getter
    @Setter
    String editor;
    
    @Getter
    @Setter
    String time;
}

