package tavernnet.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import tavernnet.exception.CharacterNotFoundException;
import tavernnet.model.Character;
import tavernnet.repository.CharacterRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);
    private final CharacterRepository characterbase;
    private final Validator validator;

    @Autowired
    public CharacterService(CharacterRepository characterbase, Validator validator) {
        this.characterbase = characterbase;
        this.validator = validator;
    }

    /**
     * @return Lista de todos los characters.
     */
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    public List<@NotNull @Valid Character> getCharacters() {
        return characterbase.findAll();
    }

    public List<@NotNull @Valid Character> getCharactersByUser(String id) {
        return characterbase.getCharactersByUser(id);
    }

    /**
     * @param id Identificador del character.
     * @return El character que tiene el id especificado.
     * @throws CharacterNotFoundException Si el character no se encuentra.
     */
    public tavernnet.model.@Valid Character getCharacter(@NotBlank String id) throws CharacterNotFoundException {
        tavernnet.model.@Valid Character character = characterbase.getCharacterById(id);
        if (character == null) {
            throw new CharacterNotFoundException(id);
        }
        return character;
    }

    /**
     * @param newCharacter Contenido del nuevo personaje a crear.
     * @return Id del nuevo character creado.
     */
    public String createCharacter(tavernnet.model.@Valid Character newCharacter) {

        // La fecha tiene que ser la de ahora
        newCharacter.setDate(LocalDateTime.now());

        if (newCharacter.getId() != null) {
            // TODO: evitar esto de alguna forma
            throw new RuntimeException("Crear character con id establecido");
        }

        // Validar ahora el resto de campos: titulo y contenido
        var violations = validator.validate(newCharacter);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        newCharacter = characterbase.save(newCharacter);
        log.info("Created character with id '{}'", newCharacter.getClass());
        return newCharacter.getId();
    }

    public void deleteCharacter(@NotBlank String characterId) {
        throw new RuntimeException("Not implemented");
    }
}

