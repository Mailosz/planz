package pl.mo.planz.services;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.ProfileRepository;
import pl.mo.planz.repositories.ValueListRepository;

@Service
public class IdentityService {

    @Autowired
    ProfileRepository profileRepository;


    @Autowired
    IdentityRepository identityRepository;


    public void adminOrThrow(String token) {
        requireProfileOrThrow(token, "admin");
    }

    public Set<String> getProfilesOrThrow(String token) {
        var identity = identityRepository.findFromToken(token);
        if (!identity.isPresent() || !identity.get().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return identity.get().getProfiles().stream().map((p) -> p.getName()).collect(Collectors.toSet());

    }


    public Set<String> getProfiles(Optional<IdentityModel> identity) {
        if (identity.isPresent()) {
            return identity.get().getProfiles().stream().map((p) -> p.getName()).collect(Collectors.toSet());
        } else {
            return new HashSet<String>();
        }
    }

    public Set<String> requireProfileOrThrow(String token, String profile) {

        Set<String> profiles = getProfilesOrThrow(token);

        if (!profiles.contains(profile)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return profiles;
    }
}
