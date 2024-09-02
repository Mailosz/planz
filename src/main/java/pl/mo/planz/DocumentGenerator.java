package pl.mo.planz;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;

public class DocumentGenerator {
    

    @Transactional
    public static void removeOldDocuments(DocumentRepository docRepo, FieldValueRepository fvRepo, FieldValueHistoryRepository histRepo) {

        LocalDate minimumDate = LocalDate.now().minusMonths(12);

        List<DocumentModel> docs = docRepo.findAll();

        for (var doc : docs) {
            if (doc.getDate().isBefore(minimumDate)) {
                if (doc.getNext() != null) {
                    doc.getNext().setPrev(null);
                    docRepo.save(doc.getNext());
                    doc.setNext(null);
                }
                if (doc.getPrev() != null) {
                    doc.getPrev().setNext(null);
                    docRepo.save(doc.getPrev());
                    doc.setPrev(null);
                }

                // var values = fvRepo.getAllForDocumentId(doc.getId());
                // for (var fieldValue : values) {
                //     histRepo.deleteHistoryForField(fieldValue.getId());
                //     // var items = value.getHistoryItems();
                //     // // for (var item : items) {
                //     // //     histRepo.delete(item);
                //     // // }
                //     fvRepo.delete(fieldValue);
                // }

                doc = docRepo.save(doc);
                docRepo.delete(doc);

                System.out.println("Document ID: " + doc.getId().toString() + " of " +  doc.getDate().toString() + " removed");
            }
        }

    }

    public static void generateDocuments(DocumentRepository docRepo, TemplateRepository templateRepo, SeriesRepository seriesRepository) {

        List<SeriesModel> series = seriesRepository.findAll();

        for (SeriesModel seriesModel : series) {
            generateDocumentsForSeries(docRepo, templateRepo, seriesModel);
        }

    }

    /**
     * Autogenerating documents
     * @param docRepo
     * @param templateRepo
     * @param series
     */
    public static void generateDocumentsForSeries(DocumentRepository docRepo, TemplateRepository templateRepo, SeriesModel series) {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue() - 1;
        LocalDate weekStart = LocalDate.now().minusDays(dayOfWeek);
        List<LocalDate> weeks = weekStart.datesUntil(weekStart.plusMonths(6), Period.ofWeeks(1)).collect(Collectors.toList());

        if (weeks.size() > 0) {

            TemplateModel defaultTemplate = series.getDefaultTemplate();

            if (defaultTemplate == null) {

                //choosing template
                List<TemplateModel> allTemplates = templateRepo.findTemplatesForSeries(series.getId());

                if (allTemplates.size() == 0) {
                    System.out.println("No template");
                    return;
                }

                defaultTemplate = allTemplates.get(0);
            }


            //creating documents
            List<DocumentModel> docs = docRepo.findAll();
            DocumentModel previous = null;
            for (var week : weeks) {

                var docOpt = docs.stream().filter((d) -> d.getDate().isEqual(week)).findFirst();
                DocumentModel doc;
                if (docOpt.isPresent()) { // setting next and prev weeks on every document

                    doc = docOpt.get();

                    if (previous != null) {
                        doc.setPrev(previous);
                        previous.setNext(doc);

                        docRepo.save(previous);
                        docRepo.save(doc);
                    }

                } else { // create new document
                    doc = new DocumentModel();

                    doc.setDate(week);
                    doc.setTemplate(defaultTemplate);
                    doc.setSeries(series);

                    doc.setPrev(previous);
                    if (previous != null) {
                        docRepo.save(doc);
                        previous.setNext(doc);
                        docRepo.save(previous);
                    } else {
                        docRepo.save(doc);
                    }
                    
                    
                    previous = doc;
                }

                previous = doc;
            };
        }
    }

}
