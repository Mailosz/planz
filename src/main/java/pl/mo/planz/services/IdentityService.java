package pl.mo.planz.services;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.ProfileAssignmentModel;
import pl.mo.planz.model.ProfileModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.ProfileAssignmentRepository;
import pl.mo.planz.repositories.ValueListRepository;

@Service
public class IdentityService {

    @Autowired
    PermissionRepository profileRepository;


    @Autowired
    IdentityRepository identityRepository;

    @Autowired
    ProfileAssignmentRepository assignmentRepository;

    @Autowired
    AccessService accessService;



    public void assignProfile(ProfileModel profile, IdentityModel identity, SeriesModel series) {

        Optional<ProfileAssignmentModel> opt = assignmentRepository.findByProfileAndIdentityAndSeries(profile, identity, series);

        if (!opt.isPresent()) {
            ProfileAssignmentModel assignment = new ProfileAssignmentModel();
            assignment.setProfile(profile);
            assignment.setIdentity(identity);
            assignment.setSeries(series);
            assignment.setSince(Instant.now());

            assignmentRepository.save(assignment);
        }
    }


}
