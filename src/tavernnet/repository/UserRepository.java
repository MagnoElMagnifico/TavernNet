package tavernnet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

import tavernnet.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // TODO: username no es _id realmente?
    @Query("{'username':  '?0'}")
    Set<User> findByUsername(String username);

    /**
     * @param user Guarda el usuario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends User> S save(S user);
}
