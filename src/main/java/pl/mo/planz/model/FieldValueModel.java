package pl.mo.planz.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "field_value")
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
    // @JoinColumn(comment = "Who last edited the current value")
    @Getter
    @Setter
    IdentityModel editIdentity;
    
    // @Column(comment = "When was the current value last edited")
    @Getter
    @Setter
    Instant editTime;

    @OneToMany(mappedBy = "field", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Getter
    @Setter
    List<FieldValueHistoryModel> historyItems;
}
