package tavernnet.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.*;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.service.UserService;
import tavernnet.service.CharacterService;
import tavernnet.model.Character;

import java.util.List;

@RestController
@RequestMapping("users")
public class CharacterController {
    private static final Logger log = LoggerFactory.getLogger(CharacterController.class);
    private final UserService userService;
    private final CharacterService characterService;

    @Autowired
    public CharacterController(UserService userService, CharacterService characterService) {
        this.userService = userService;
        this.characterService = characterService;
    }

    /**
     * <code>POST /users/{userid}/characters/</code>
     * @param newCharacter Nueva publicación.
     * @return <code>201 Created</code> en éxito.
     */
    @PostMapping("{userid}/characters")
    public ResponseEntity<Void> createCharacter(
        @PathVariable("userid") @NotBlank String userId, // TODO: unused
        @RequestBody Character.CreationRequest newCharacter
    ) throws DuplicatedResourceException {

            String newId = characterService.createCharacter(newCharacter, userId);

            var url = MvcUriComponentsBuilder.fromMethodName(
                    CharacterController.class,
                    "getCharacter",
                    userId,
                    newId)
                .build()
                .toUri();

            return ResponseEntity.created(url).build();
    }

    // Servicio para obtener todos los personajes de un usuario
    @GetMapping("{userid}/characters")
    public List<Character> getCharacters(@PathVariable("userid") String id) {
        // TODO: user not found
        return characterService.getCharactersByUser(id);
    }


    /**
     * @param userId Nombre del usuario al que pertenece el personaje
     * @param characterName Nombre del personaje a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    @DeleteMapping("{userid}/characters/{characterName}")
    public ResponseEntity<Void> deleteCharacter(
        @PathVariable("userid") String userId, @PathVariable("characterName")
        String characterName
    ) throws ResourceNotFoundException {
        characterService.deleteCharacter(userId, characterName);
        return ResponseEntity.noContent().build();
    }


    /**
     * <code>GET /users/{userid}/characters/{characterName}</code>
     * @param userId Identificador del usuario.
     * @param characterName Nombre del character.
     * @return Personaje solicitado
     */
    @GetMapping("{userid}/characters/{characterName}")
    public Character getCharacter(
        @PathVariable("userid") @NotBlank String userId,
        @PathVariable("characterName") @NotBlank String characterName
    ) throws ResourceNotFoundException {
        return characterService.getCharacter(userId, characterName);
    }

}

