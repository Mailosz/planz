package pl.mo.planz.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "token")
public class TokenModel {
    @Id
    @Getter
    @Setter
    String value;

    @ManyToOne
    @Getter
    @Setter
    IdentityModel identity;

    @ManyToOne
    @Getter
    @Setter
    SeriesModel series;

    @Getter
    @Setter
    boolean active = true;
}
