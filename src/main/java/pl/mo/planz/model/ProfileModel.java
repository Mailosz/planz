package pl.mo.planz.model;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profile")
public class ProfileModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    @Column(unique = true)
    String name;

    @Getter
    @Setter
    @Column(unique = true)
    String description;

    @ManyToMany
    @Getter
    @Setter
    Set<PermissionModel> permissions; 

    @OneToMany(mappedBy = "profile", cascade = CascadeType.REMOVE)
    @Getter
    @Setter
    Set<ProfileAssignmentModel> assignments; 
}
