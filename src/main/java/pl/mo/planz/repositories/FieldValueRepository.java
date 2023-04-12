package pl.mo.planz.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.FieldValueModel;

public interface FieldValueRepository extends JpaRepository<FieldValueModel, UUID> {
    
    @Query("select fv from FieldValueModel fv where document.id = ?1")
    List<FieldValueModel> getAllForDocumentId(UUID docId);

    @Query("select fv from FieldValueModel fv where document.id = ?1 and field.id = ?2")
    Optional<FieldValueModel> findByDocAndField(UUID docId, UUID fieldId);

    @Modifying
    @Query("delete from FieldValueModel fv where fv.document.id = ?1")
    void deleteAllForDocumentId(UUID docId);
}
