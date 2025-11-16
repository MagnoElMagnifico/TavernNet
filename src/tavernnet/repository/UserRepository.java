package tavernnet.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import tavernnet.model.User;

@Repository
@NullMarked
public interface UserRepository extends MongoRepository<User, String> {

    @Query("{'_id':  '?0'}")
    Optional<User> findByUsername(String username);

    // No usar deleteById ya que ignora si no existe
    @Query(value = "{ '_id': ?0 }", delete = true)
    Optional<User> deleteUserById(String id);

    /**
     * @param user Guarda el usuario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends User> S save(S user);
}
