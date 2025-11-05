package tavernnet.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

import tavernnet.model.Post;
import tavernnet.model.User;

@Repository
public interface UserRepository extends MongoRepository<@NotNull User, @NotBlank String>{
    @Query("{'username':  '?0'}")
    Set<User> findByUsername(@NotNull String username);

    // No usar deleteById ya que ignora si no existe
    @Query(value = "{ '_id': ?0 }", delete = true)
    User deleteUserById(String id);

    /**
     * @param user Guarda el usuario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull User> S save(@Valid S user);
}
