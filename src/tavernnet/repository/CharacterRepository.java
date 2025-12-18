package tavernnet.repository;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

import tavernnet.model.Character;

@Repository
public interface CharacterRepository extends MongoRepository<Character, ObjectId> {
    @Query("{ 'user': ?0 }")
    Collection<Character> getCharactersByUser(String username);

    @Query(value = "{ 'user': ?0, 'name': ?1 }", exists = true)
    boolean existsByName(String username, String characterName);

    @Query("{ 'user': ?0, 'name': ?1 }")
    Character getCharacterByName(String username, String characterName);

    @Query("{ '_id': ?0 }")
    Character getCharacterById(ObjectId characterid);

    @Query(value = "{ '_id': ?0 }", delete = true)
    Character deleteCharacterById(ObjectId characterid);

    @Query(value = "{ 'user': ?0 }", count = true)
    int countUserCharacters(String username);

    <S extends Character> S save(S character);
}
