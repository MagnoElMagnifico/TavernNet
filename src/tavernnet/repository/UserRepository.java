package tavernnet.repository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tavernnet.model.User;

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<@NotNull User, @NotBlank String>{
    @Query("{'username':  '?0'}")
    Set<User> findByUsername(@NotNull String username);

    /**
     * @param user Guarda el usuario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull User> S save(@Valid S user);
}
