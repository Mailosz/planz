package pl.mo.planz.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.DocumentModel;

public interface DocumentRepository extends JpaRepository<DocumentModel, UUID>{
    
    @Query("select doc from DocumentModel doc where doc.date < ?1 order by doc.date desc")
    Optional<DocumentModel> findCurrentForDate(LocalDate date);

    List<DocumentModel> findDocumentsBySeriesId(UUID seriesId);

}
