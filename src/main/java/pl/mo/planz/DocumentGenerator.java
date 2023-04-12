package pl.mo.planz;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.TemplateRepository;

public class DocumentGenerator {
    

    public static void generateDocumentsAndRemoveOld(DocumentRepository docRepo, TemplateRepository templateRepo, boolean removeOldDocs) {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue() - 1;
        LocalDate weekStart = LocalDate.now().minusDays(dayOfWeek);
        List<LocalDate> weeks = weekStart.datesUntil(weekStart.plusMonths(2), Period.ofWeeks(1)).collect(Collectors.toList());
        LocalDate minimumDate = LocalDate.now().minusMonths(2);

        List<DocumentModel> docs = docRepo.findAll();

        if (removeOldDocs) {
            for (var doc : docs) {
                if (doc.getWeek().isBefore(minimumDate)) {
                    if (doc.getNext() != null) doc.getNext().setPrev(null);
                    if (doc.getPrev() != null) doc.getPrev().setNext(null);
                    docRepo.delete(doc);
                }
            }
        }

        DocumentModel previous = null;
        for (var week : weeks) {

            var docOpt = docs.stream().filter((d) -> d.getWeek().isEqual(week)).findFirst();
            DocumentModel doc;
            if (docOpt.isPresent()) {

                doc = docOpt.get();

                if (previous != null) {
                    doc.setPrev(previous);
                    previous.setNext(doc);

                    docRepo.save(previous);
                    docRepo.save(doc);
                }

            } else { // create new document
                doc = new DocumentModel();

                doc.setWeek(week);
                doc.setTemplate(templateRepo.findAll().get(0));

                doc.setPrev(previous);
                if (previous != null) {
                    previous.setNext(doc);
                    docRepo.save(previous);
                }
                
                docRepo.save(doc);
                previous = doc;
            }

            previous = doc;
        };
    }
}
