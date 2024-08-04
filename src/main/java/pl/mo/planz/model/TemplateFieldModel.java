package pl.mo.planz.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "template_field")
public class TemplateFieldModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @ManyToOne
    @Getter
    @Setter
    TemplateModel template;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    int pos;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    FieldType type = FieldType.TEXT;

    @ManyToOne
    @Getter
    @Setter
    DatalistModel datalist;

    @ManyToOne
    @Getter
    @Setter
    PermissionModel editProfile;

    @Column(length = 1000000)
    @Getter
    @Setter
    String defaultValue = "";

    @Getter
    @Setter
    boolean isPublic = true;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Getter
    @Setter
    List<FieldValueModel> values;
}
