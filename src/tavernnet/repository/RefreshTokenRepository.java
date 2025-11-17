package tavernnet.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tavernnet.model.RefreshToken;

import java.util.Optional;

@Repository
@NullMarked
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    void deleteAllByUser(String user);
    Optional<RefreshToken> findByToken(String token);
}
