package tavernnet.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tavernnet.model.UserRefreshToken;

import java.util.Optional;

@Repository
@NullMarked
public interface UserRefreshTokenRepository  extends CrudRepository<UserRefreshToken, String> {
    void deleteById(String username);
    Optional<UserRefreshToken> findById(String username);
}
