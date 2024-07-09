package pl.mo.planz.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.DatalistValueModel;
import pl.mo.planz.model.DatalistModel;

public interface DatalistRepository extends JpaRepository<DatalistModel, UUID> {
    
    @Query("select list from DatalistModel list where list.name = ?1")
    Optional<DatalistModel> findByName(String name);

    @Modifying
    @Query("delete from DatalistValueModel dv where dv.list = ?1")
    void deleteAllValues(UUID datalistId);
}
