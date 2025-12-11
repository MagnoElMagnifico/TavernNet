package tavernnet.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.LimitException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Character;
import tavernnet.repository.CharacterRepository;
import tavernnet.repository.UserRepository;
import tavernnet.utils.patch.JsonPatch;
import tavernnet.utils.patch.JsonPatchOperation;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;

@Service
@NullMarked
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);
    private final CharacterRepository charRepo;
    private final UserRepository userRepo;
    private final ObjectMapper mapper;
    private final Validator validator;

    @Autowired
    public CharacterService(
        CharacterRepository charRepo,
        UserRepository userRepo,
        ObjectMapper mapper,
        Validator validator
    ) {
        this.charRepo = charRepo;
        this.userRepo = userRepo;
        this.mapper = mapper;
        this.validator = validator;
    }

    public Collection<Character.PublicCharacter> getCharactersByUser(String username) throws ResourceNotFoundException {
        log.debug("GET /users/{}/characters", username);
        if (!userRepo.existsById(username)) {
            throw new ResourceNotFoundException("User", username);
        }
        return charRepo.getCharactersByUser(username).stream().map(Character.PublicCharacter::new).toList();
    }

    /**
     * @param username Identificador del usuario.
     * @param characterName Nombre del personaje
     * @return El character que tiene el id especificado.
     * @throws ResourceNotFoundException Si el character no se encuentra.
     */
    public Character.PublicCharacter getCharacter(
        String username,
        String characterName
    ) throws ResourceNotFoundException {
        log.debug("GET /users/{}/characters/{}", username, characterName);

        if (!userRepo.existsById(username)) {
            throw new ResourceNotFoundException("User", username);
        }

        Character character = charRepo.getCharacterByName(username, characterName);
        if (character == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }

        return new Character.PublicCharacter(character);
    }

    /**
     * @param newCharacter Contenido del nuevo personaje a crear.
     * @return Id del nuevo character creado.
     */
    public String createCharacter(
        Character.CreationRequest newCharacter,
        String username
    ) throws DuplicatedResourceException, ResourceNotFoundException, LimitException {
        log.debug("POST /users/{}/characters", username);

        // Comprobar que el usuario existe
        if (!userRepo.existsById(username)) {
            throw new ResourceNotFoundException("User", username);
        }

        // El personaje debe ser nuevo
        if (charRepo.existsByName(username, newCharacter.name())) {
            throw new DuplicatedResourceException(newCharacter, "Character", newCharacter.name());
        }

        // Comprobar que el usuario tiene menos del numero de personajes permitidos
        if (charRepo.countUserCharacters(username) >= 10) {
            log.debug("POST /users/{}/characters user has already 10 characters", username);
            throw new LimitException("Cannot create more than 10 characters");
        }

        Character realCharacter = charRepo.save(new Character(newCharacter, username));
        log.debug("POST /users/{}/characters id='{}'", username, realCharacter.getClass());
        return realCharacter.id().toHexString();
    }

    public Character.PublicCharacter updateCharacter(
        String username,
        String characterName,
        List<JsonPatchOperation> changes
    ) throws ResourceNotFoundException, JsonPatchFailedException {
        log.debug("PATCH /users/{}/characters/{}", username, characterName);

        if (!userRepo.existsById(username)){
            throw new ResourceNotFoundException("User", username);
        }

        Character character = charRepo.getCharacterByName(username, characterName);
        if (character == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }

        // Comprobar que las operaciones estan permitidas
        for (JsonPatchOperation operation : changes) {
            Character.validatePatch(operation);
        }

        // Crear un objeto con los nuevos cambios
        JsonNode updatedNode = JsonPatch.apply(
            changes,
            mapper.convertValue(character, JsonNode.class)
        );
        Character updated = mapper.convertValue(updatedNode, Character.class);
        assert updated.id() == character.id() && updated.creation() == character.creation();
        Character newCharacter = new Character(
            character.id(), // conservar del original
            updated.name(),
            character.user(), // conservar del original
            updated.biography(),
            updated.race(),
            updated.languages(),
            character.creation(), // conservar del original
            updated.alignment(),
            updated.stats(),
            updated.modifiers(),
            updated.combat(),
            updated.passive(),
            updated.actions()
        );

        // Validar los campos manualmente
        Set<ConstraintViolation<Character>> violations = validator.validate(updated);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new JsonPatchFailedException("Invalid fields: " + msg);
        }

        // Se debe hacer asi o MongoDB tratara de insertarlo como un nuevo documento
        charRepo.save(newCharacter);
        return new Character.PublicCharacter(newCharacter);
    }

    public void deleteCharacter(
        String username, String characterName
    ) throws ResourceNotFoundException {
        log.debug("DELETE /users/{}/characters/{}", username, characterName);
        Character deletedCharacter = charRepo.getCharacterByName(username, characterName);
        if (deletedCharacter == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }
        charRepo.deleteCharacterById(deletedCharacter.id());
    }
}
