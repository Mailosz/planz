package pl.mo.planz.model;

import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class DatalistValueModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @ManyToOne
    @Getter
    @Setter
    DatalistModel list;

    @Getter
    @Setter
    String value;

    public DatalistValueModel(String value, DatalistModel datalist) {
        this.value = value;
        list = datalist;
    }
}
