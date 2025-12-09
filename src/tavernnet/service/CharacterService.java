package tavernnet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;
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
import tavernnet.repository.UserRepository;
import tavernnet.utils.patch.JsonPatch;
import tavernnet.utils.patch.JsonPatchOperation;
import tavernnet.utils.patch.JsonPatchOperationType;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;

@Service
@NullMarked
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);
    private final CharacterRepository charRepo;
    private final UserRepository userRepo;
    private final ObjectMapper mapper;

    @Autowired
    public CharacterService(
        CharacterRepository charRepo,
        UserRepository userRepo,
        ObjectMapper mapper
    ) {
        this.charRepo = charRepo;
        this.userRepo = userRepo;
        this.mapper = mapper;
    }

    /**
     * @return Lista de todos los characters.
     */
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    public Collection<Character> getCharacters() {
        return charRepo.findAll();
    }

    public Collection<Character> getCharactersByUser(String username) throws ResourceNotFoundException {
        if (!userRepo.existsById(username)) {
            throw new ResourceNotFoundException("User", username);
        }
        return charRepo.getCharactersByUser(username);
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
        @Valid Character character = charRepo
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
    public String createCharacter(Character.CreationRequest newCharacter, String username) throws DuplicatedResourceException, ResourceNotFoundException {
        // Comprobar que el usuario existe
        if (!userRepo.existsById(username)) {
            throw new ResourceNotFoundException("User", username);
        }

        // TODO: limitar personajes a un maximo de 10 por usuario y evitar tener que hacer paginacion

        // El personaje debe ser nuevo
        if (charRepo.existsByName(username, newCharacter.name())) {
            throw new DuplicatedResourceException(newCharacter, "Character", newCharacter.name());
        }

        Character realCharacter = charRepo.save(new Character(newCharacter, username));
        log.info("Created character with id '{}'", realCharacter.getClass());
        return realCharacter.getId().toHexString();
    }

    public Character updateCharacter(@NotBlank String username, @NotBlank String characterName, List<JsonPatchOperation> changes)
        throws ResourceNotFoundException, JsonPatchFailedException {
        Character character = charRepo.getCharacterByName(username, characterName);
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
        return charRepo.save(updated);
    }

    public void deleteCharacter(@NotBlank String username, @NotBlank String characterName)
        throws ResourceNotFoundException {
        Character  deletedCharacter = charRepo.getCharacterByName(username, characterName);
        if (deletedCharacter == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }
        charRepo.deleteCharacterById(deletedCharacter.getId());

    }


}

