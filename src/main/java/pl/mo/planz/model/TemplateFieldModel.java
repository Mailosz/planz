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

    @ManyToOne
    @Getter
    @Setter
    ValueListModel datalist;

    @ManyToOne
    @Getter
    @Setter
    ProfileModel editProfile;

    @Getter
    @Setter
    String autoMethod;

    @Getter
    @Setter
    String defaultValue = "";

    @Getter
    @Setter
    boolean isPublic = true;
}
