package pl.mo.planz.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class FieldValueModel {

    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Getter
    @Setter
    @ManyToOne
    DocumentModel document;

    @ManyToOne
    @Getter
    @Setter
    TemplateFieldModel field;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    FieldType type = FieldType.TEXT;

    @Column(length = 1000000)
    @Getter
    @Setter
    String value;

    @ManyToOne
    @Getter
    @Setter
    IdentityModel editIdentity;
    
    @Getter
    @Setter
    Instant editTime;
}
