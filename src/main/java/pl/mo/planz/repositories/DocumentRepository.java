package pl.mo.planz.repositories;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.DocumentModel;

public interface DocumentRepository extends JpaRepository<DocumentModel, UUID>{
    
    @Query("select doc from DocumentModel doc where doc.week < ?1 order by doc.week desc")
    Optional<DocumentModel> findCurrentForDate(LocalDate date);

}
