package pl.mo.planz.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.ProfileAssignmentModel;
import pl.mo.planz.model.ProfileModel;
import pl.mo.planz.model.SeriesModel;

public interface ProfileAssignmentRepository extends JpaRepository<ProfileAssignmentModel, UUID> {
    
    @Query("select assignment from ProfileAssignmentModel assignment where assignment.profile = ?1 and assignment.identity = ?2 and assignment.series = ?3")
    Optional<ProfileAssignmentModel> findByProfileAndIdentityAndSeries(ProfileModel profileId, IdentityModel identityId, SeriesModel seriesId);

}
