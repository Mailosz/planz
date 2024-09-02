package pl.mo.planz.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.TemplateModel;

public interface TemplateRepository extends JpaRepository<TemplateModel, UUID> {
    
    @Query("select id from TemplateModel")
    List<UUID> getAllIds();

    @Query("select tm from TemplateModel tm where tm.series.id = ?1 ")
    List<TemplateModel> findTemplatesForSeries(UUID seriesId);
}
