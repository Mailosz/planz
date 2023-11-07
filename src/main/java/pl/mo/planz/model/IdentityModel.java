package pl.mo.planz.model;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class IdentityModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    boolean isActive = true;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    IdentityType type = IdentityType.TOKEN;

    @ManyToMany
    @Getter
    @Setter
    Set<ProfileModel> profiles; 
}
