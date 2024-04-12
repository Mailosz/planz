package pl.mo.planz.services;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.PermissionModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TokenModel;

@Getter
@Setter
public class AccessObject {

    private TokenModel token;
    private IdentityModel identity;
    private SeriesModel series;
    private Set<String> permissions;

}
