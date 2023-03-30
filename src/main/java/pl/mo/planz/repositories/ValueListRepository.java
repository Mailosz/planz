package pl.mo.planz.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.ProfileModel;
import pl.mo.planz.model.ValueListItemModel;
import pl.mo.planz.model.ValueListModel;

public interface ValueListRepository extends JpaRepository<ValueListModel, UUID> {
    
    @Query("select list from ValueListModel list where list.name = ?1")
    Optional<ValueListModel> findByName(String name);
}
