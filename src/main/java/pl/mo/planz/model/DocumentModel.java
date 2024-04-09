package pl.mo.planz.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class DocumentModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    LocalDate week;

    @ManyToOne
    @Getter
    @Setter
    TemplateModel template;

    @ManyToOne
    @Getter
    @Setter
    SeriesModel series;

    @OneToOne
    @Getter
    @Setter
    DocumentModel prev;

    @OneToOne
    @Getter
    @Setter
    DocumentModel next;

    @Getter
    @Setter
    boolean isPublic = false;

    @Getter
    @Setter
    boolean isEditable = false;

    @Column(length = 1000000)
    @Getter
    @Setter
    String generatedContent;

    @Getter
    @Setter
    Instant generatedTime;

    @OneToMany(mappedBy = "document", cascade = CascadeType.REMOVE)
    @Getter
    @Setter
    List<FieldValueModel> values;
}
