package pl.mo.planz.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.TokenModel;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.ProfileRepository;
import pl.mo.planz.repositories.TokenRepository;

@Service
public class AccessService {

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    PermissionRepository permissionRepository;

    public AccessObject getAccess(String token, String... allowingPermissions) {

        var access = getAccess(token);

        if (Collections.disjoint(access.getPermissions(), Arrays.asList(allowingPermissions))) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403), "No permission");
        }

        return access;
    }
    
    public AccessObject getAccess(String token) {
        Optional<TokenModel> tokenModelOpt = tokenRepository.findById(token);

        if (!tokenModelOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403), "Wrong token");
        }

        IdentityModel identity = tokenModelOpt.get().getIdentity();

        Set<String> permissions = permissionRepository.findAllByAccess(identity, tokenModelOpt.get().getSeries()).stream().map((p) -> p.getName()).collect(Collectors.toSet());

        AccessObject access = new AccessObject();
        access.setToken(tokenModelOpt.get());
        access.setIdentity(identity);
        access.setSeries(tokenModelOpt.get().getSeries());
        access.setPermissions(permissions);
        return access;
    }

    public void adminOrThrow(String token) {
        getAccess(token, "admin");
    }
}
