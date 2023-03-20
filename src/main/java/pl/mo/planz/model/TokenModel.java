package pl.mo.planz.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class TokenModel {
    @Id
    @Getter
    @Setter
    String value;

    @OneToOne
    @Getter
    @Setter
    IdentityModel identity;
}
