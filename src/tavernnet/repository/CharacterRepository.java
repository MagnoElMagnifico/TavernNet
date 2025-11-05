package tavernnet.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import tavernnet.model.Character;
import tavernnet.model.Post;

@Repository
public interface CharacterRepository extends MongoRepository<@NotNull Character, @NotBlank String> {
    /**
     * @return Lista de todos los posts en la base de datos.
     */
    @Query("{}")
    List<@NotNull @Valid Character> getCharacters();

    @Query("{ 'user': '?0' }")
    List<@Valid Character> getCharactersByUser(String userid);

    /**
     * @param characterid Id del personaje del que obtener los datos.
     * @return PostView que tiene el ID dato o <code>null</code> si no existe.
     */
    @Query("{ '_id': '?0' }")
    @Valid
    Character getCharacterById(String characterid);

    // No usar deleteById ya que ignora si no existe
    @Query(value = "{ '_id': ?0 }", delete = true)
    Character deleteCharacterById(String characterid);

    /**
     * @param character Guarda el personaje en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull Character> S save(@Valid S character);

}
