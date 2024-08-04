package pl.mo.planz.model;

import java.time.Period;
import java.util.UUID;

// import org.springframework.data.convert.Jsr310Converters.PeriodToStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pl.mo.planz.converters.PeriodToStringConverter;

@Entity
@Table(name = "series")
public class SeriesModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Column(length = 1000)
    @Getter
    @Setter
    String name;

    @ManyToOne
    @Getter
    @Setter
    DocumentModel currentDocument;

    @ManyToOne
    @Getter
    @Setter
    DocumentModel firstDocument;

    @ManyToOne
    @Getter
    @Setter
    DocumentModel lastDocument;

    @Getter
    @Setter
    @Convert(converter = PeriodToStringConverter.class)
    Period generationInterval;

    @ManyToOne
    @Getter
    @Setter
    TemplateModel defaultTemplate;
}
