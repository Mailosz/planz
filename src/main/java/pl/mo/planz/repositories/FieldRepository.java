package pl.mo.planz.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;

public interface FieldRepository extends JpaRepository<TemplateFieldModel, UUID> {
    
    @Modifying
    @Query("delete from TemplateFieldModel tmf where tmf.template is null")
    void deleteAllOrphanedFields();

}
