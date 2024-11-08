package pl.mo.planz.model;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permission")
public class PermissionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    @Column(unique = true, nullable = false)
    String name;

    @Getter
    @Setter
    String description;

    @ManyToMany(mappedBy = "permissions")
    @Getter
    @Setter
    Set<ProfileModel> profiles; 

    @OneToMany(mappedBy = "editPermission", fetch = FetchType.LAZY)
    @Getter
    @Setter
    Set<TemplateFieldModel> fields;
}
