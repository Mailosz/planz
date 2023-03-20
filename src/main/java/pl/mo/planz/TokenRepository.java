package pl.mo.planz;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.mo.planz.model.TokenModel;

public interface TokenRepository extends JpaRepository<TokenModel, UUID> {

}
