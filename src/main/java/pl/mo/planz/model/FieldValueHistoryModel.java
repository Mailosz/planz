package pl.mo.planz.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class FieldValueHistoryModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    @Getter
    @Setter
    FieldValueModel field;

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
