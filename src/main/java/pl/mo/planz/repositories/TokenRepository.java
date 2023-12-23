package pl.mo.planz.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import pl.mo.planz.model.TokenModel;

public interface TokenRepository extends JpaRepository<TokenModel, String> {

}
