package pl.mo.planz.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.DatalistValueModel;
import pl.mo.planz.model.DatalistModel;

public interface DatalistValueRepository extends JpaRepository<DatalistValueModel, UUID> {

    @Modifying
    @Query("delete from DatalistValueModel dv where dv.list = ?1")
    void deleteAllValues(DatalistModel datalistId);
}
