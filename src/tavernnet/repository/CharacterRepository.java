package tavernnet.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import tavernnet.model.Character;

@Repository
public interface CharacterRepository extends MongoRepository<Character, String> {
    /**
     * @return Lista de todos los posts en la base de datos.
     */
    @Query("{}")
    List<Character> getCharacters();

    @Query("{ 'user': '?0' }")
    List<Character> getCharactersByUser(String userid);

    /**
     * @param characterid Id del personaje del que obtener los datos.
     * @return PostView que tiene el ID dato o <code>null</code> si no existe.
     */
    @Query("{ '_id': '?0' }")
    Character getCharacterById(String characterid);

    @Query(value = "{ '_id': '?0' }", delete = true)
    void deleteCharacterById(String characterid);

    /**
     * @param character Guarda el personaje en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends Character> S save(S character);
}
