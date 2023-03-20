package pl.mo.planz;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.TemplateModel;

public interface TemplateRepository extends JpaRepository<TemplateModel, UUID> {
    
    @Query("select id from TemplateModel")
    List<UUID> getAllIds();
}
