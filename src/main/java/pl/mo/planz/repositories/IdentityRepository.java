package pl.mo.planz.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.IdentityModel;

public interface IdentityRepository extends JpaRepository<IdentityModel, UUID> {

    @Query("select identity from TokenModel where value = ?1")
    Optional<IdentityModel> findFromToken(String token);
}
