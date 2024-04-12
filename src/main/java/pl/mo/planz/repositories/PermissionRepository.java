package pl.mo.planz.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.SeriesModel;

public interface PermissionRepository extends JpaRepository<PermissionModel, UUID> {
    
    @Query("select permission from PermissionModel permission where permission.name = ?1")
    Optional<PermissionModel> findByName(String name);

    // @Query("select permission from PermissionModel permission inner join permission.profiles profile inner join profile.assignments assignment where assignment.identity = ?1 and assignment.series = ?2 inner join permission.profiles profile inner join profile.assignments assignment where assignment.identity = ?1 and assignment.series = ?2")
    @Query("select permission from PermissionModel permission")
    List<PermissionModel> findAllByAccess(IdentityModel identity, SeriesModel series);
}
