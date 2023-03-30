package pl.mo.planz.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.FieldValueHistoryModel;
import pl.mo.planz.model.FieldValueModel;

public interface FieldValueHistoryRepository extends JpaRepository<FieldValueHistoryModel, UUID> {
    
    @Query("select fvh from FieldValueHistoryModel fvh where field.id = ?1 order by editTime")
    List<FieldValueHistoryModel> getHistoryForField(UUID docId);

}
