package pl.mo.planz.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.ProfileModel;

public interface ProfileRepository extends JpaRepository<ProfileModel, UUID> {
    
    @Query("select prof from ProfileModel prof where prof.name = ?1")
    Optional<ProfileModel> findByName(String name);
}
