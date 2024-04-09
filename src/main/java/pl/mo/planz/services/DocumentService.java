package pl.mo.planz.services;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.SeriesRepository;

@Service
public class DocumentService {

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    SeriesRepository seriesRepository;
    

    public DocumentModel createForSeries(SeriesModel series) {

        LocalDate date = LocalDate.now();
        if (series.getLastDocument() != null && series.getGenerationInterval() != null) {

            date = series.getLastDocument().getWeek().plus(series.getGenerationInterval());

        }

        DocumentModel document = new DocumentModel();
        document.setSeries(series);
        document.setWeek(date);
        document.setPrev(series.getLastDocument());
        document.setTemplate(series.getDefaultTemplate());

        documentRepository.save(document);

        series.setLastDocument(document);
        seriesRepository.save(series);

        return document;
    }
}
