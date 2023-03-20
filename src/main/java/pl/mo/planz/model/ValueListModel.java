package pl.mo.planz.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
public class ValueListModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    UUID id;

    @Getter
    @Setter
    String name;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL)
    @Getter
    @Setter
    List<ValueListItemModel> values;

    public void addValue(String value) {
        ValueListItemModel vli = new ValueListItemModel();
        vli.setList(this);
        vli.setValue(value);
        values.add(vli);
    }
}
