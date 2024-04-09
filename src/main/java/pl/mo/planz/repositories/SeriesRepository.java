package pl.mo.planz.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TemplateModel;

public interface SeriesRepository extends JpaRepository<SeriesModel, UUID> {
    

}
