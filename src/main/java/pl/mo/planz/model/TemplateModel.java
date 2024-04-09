package pl.mo.planz.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
public class TemplateModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    boolean isDefault = false;

    @Getter
    @Setter
    boolean active = true;

    @Getter
    @Setter
    @Column(length = 1000000)
    String content;

    @OneToMany(mappedBy = "template", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Getter
    @Setter
    List<TemplateFieldModel> fields;
}
