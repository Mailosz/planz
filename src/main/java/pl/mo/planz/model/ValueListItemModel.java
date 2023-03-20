package pl.mo.planz.model;

import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class ValueListItemModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @ManyToOne
    @Getter
    @Setter
    ValueListModel list;

    @Getter
    @Setter
    String value;
}
