package tavernnet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Character;
import tavernnet.repository.CharacterRepository;
import tavernnet.utils.patch.JsonPatch;
import tavernnet.utils.patch.JsonPatchOperation;
import tavernnet.utils.patch.JsonPatchOperationType;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;

import java.time.LocalDateTime;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);
    private final CharacterRepository characterbase;
    private final ObjectMapper mapper;

    @Autowired
    public CharacterService(CharacterRepository characterbase, ObjectMapper mapper) {
        this.characterbase = characterbase;
        this.mapper = mapper;
    }

    /**
     * @return Lista de todos los characters.
     */
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    public Collection<Character> getCharacters() {
        return characterbase.findAll();
    }

    public Collection<Character> getCharactersByUser(String id) {
        return characterbase.getCharactersByUser(id);
    }

    /**
     * @param userid Identificador del usuario.
     * @param characterName Nombre del personaje
     * @return El character que tiene el id especificado.
     * @throws ResourceNotFoundException Si el character no se encuentra.
     */
    public @Valid Character getCharacter(@NotBlank String userid,
                                         @NotBlank String characterName
    ) throws ResourceNotFoundException {
        @Valid Character character = characterbase
            .getCharacterByName(userid, characterName);
        if (character == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }
        return character;
    }

    /**
     * @param newCharacter Contenido del nuevo personaje a crear.
     * @return Id del nuevo character creado.
     */
    public String createCharacter(Character.CreationRequest newCharacter, String userId) throws DuplicatedResourceException {

        // El personaje debe ser nuevo
        if (characterbase.existsByName(userId, newCharacter.name())) {
            throw new DuplicatedResourceException(newCharacter, "Character", newCharacter.name());
        }
        Character realCharacter = new Character(newCharacter, userId);
        // La fecha de creacion tiene que ser la de ahora
        realCharacter.setDate(LocalDateTime.now());


        realCharacter = characterbase.save(realCharacter);
        log.info("Created character with id '{}'", realCharacter.getClass());
        return realCharacter.getId().toHexString();
    }

    public Character updateCharacter(@NotBlank String username, @NotBlank String characterName, List<JsonPatchOperation> changes)
        throws ResourceNotFoundException, JsonPatchFailedException {
        Character character = characterbase.getCharacterByName(username, characterName);
        if (character == null){
            throw new ResourceNotFoundException("Character", characterName);
        }

        for(JsonPatchOperation operation: changes){
            if( (operation.operation() != JsonPatchOperationType.REPLACE)
                || operation.path().toString().contains("/stats") )
                throw new JsonPatchFailedException(
                    "Operation %s on %s %s forbidden".formatted(
                        operation.operation().toString(),
                        "Character", characterName));
        }

        JsonNode updated_node = JsonPatch.apply(changes, mapper.convertValue(
            character, JsonNode.class));
        Character updated = mapper.convertValue(updated_node, Character.class);
        return characterbase.save(updated);
    }

    public void deleteCharacter(@NotBlank String username, @NotBlank String characterName)
        throws ResourceNotFoundException {
        Character  deletedCharacter = characterbase.getCharacterByName(username, characterName);
        if (deletedCharacter == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }
        characterbase.deleteCharacterById(deletedCharacter.getId());

    }


}

