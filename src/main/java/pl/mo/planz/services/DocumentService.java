package pl.mo.planz.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.FieldValueModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.view.PageBuilder;

@Service
public class DocumentService {

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    FieldValueRepository fieldValueRepository;
    

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

        if (series.getLastDocument() != null) {
            series.getLastDocument().setNext(document);
        }

        series.setLastDocument(document);
        if (series.getFirstDocument() == null) {
            series.setFirstDocument(document);
        }
        seriesRepository.save(series);

        return document;
    }

        /**
     * Generates document's content
     * @param doc
     * @return Document's generated content
     */
    public String generateDocumentContent(DocumentModel doc) {

        Map<String, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getName(), (fv) -> fv, (val1, val2) -> {return val2;}));
        String content = PageBuilder.buildTemplateForView(doc, valueMap);

        doc.setGeneratedContent(content);
        doc.setGeneratedTime(Instant.now());
        documentRepository.save(doc);

        return content;

    }

    /**
     * Finds current document in a series, always from scratch
     * @param series
     * @return
     */
    public Optional<DocumentModel> findCurrentDocument(SeriesModel series) {

        DocumentModel currentDocument = series.getCurrentDocument();

        if (currentDocument == null) {
            currentDocument = series.getFirstDocument();
            if (currentDocument == null) {
                return Optional.empty();
            }
        }

        while (currentDocument.getNext() != null) {
            DocumentModel next = currentDocument.getNext();

            if (next.getWeek().isAfter(LocalDate.now())) {
                break;
            }

            currentDocument = next;
        }

        if (series.getCurrentDocument() == null || !series.getCurrentDocument().getId().equals(currentDocument.getId())) {
            series.setCurrentDocument(currentDocument);

            seriesRepository.save(series);
        }

        return Optional.of(currentDocument);
    }

    
}
