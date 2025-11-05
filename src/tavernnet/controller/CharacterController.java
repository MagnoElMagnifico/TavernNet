package tavernnet.controller;

import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.*;
import tavernnet.model.Post;
import tavernnet.service.UserService;
import tavernnet.service.CharacterService;
import tavernnet.model.Character;
import tavernnet.model.User;
import tavernnet.service.CharacterService;
import tavernnet.service.UserService;

import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("users")
public class CharacterController {
    UserService userService;
    CharacterService characterService;
    private static final Logger log = LoggerFactory.getLogger(CharacterController.class);

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
    public ResponseEntity<Void> createCharacter(@PathVariable("userid") String userId, @RequestBody Character newCharacter) {

        try{
            String newId = characterService.createCharacter(newCharacter);

            var url = MvcUriComponentsBuilder.fromMethodName(
                    CharacterController.class,
                    "getCharacter",
                    userId,
                    newId)
                .build()
                .toUri();

            return ResponseEntity.created(url).build();
        }catch(DuplicatedCharacterException e){
            log.info("Personaje duplicado :P");
            return ResponseEntity.notFound().build();
        }

    }

    // Servicio para obtener todos los personajes de un usuario
    @GetMapping("{userid}/characters")
    public ResponseEntity<@NonNull List<Character>> getCharacters(@PathVariable("userid") String id) {
        return ResponseEntity.ok(characterService.getCharactersByUser(id));
    }

    /**
     * <code>GET /users/{userid}/characters/{characterid}</code>
     * @param userId Identificador del usuario.
     * @param characterId Identificador del character.
     * @return <code>200 OK</code> con el post solicitado, <code>404 Not
     * found</code> si no existe el ID proporcionado.
     */
    @GetMapping("{userid}/characters/{characterid}")
    public ResponseEntity<@Valid Character> getCharacter(
        @PathVariable("userid") String userId,
        @PathVariable("characterid") String characterId) {
        try {
            return ResponseEntity.ok(characterService.getCharacter(characterId));
        } catch (CharacterNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * @param characterId Identificador del personaje a borrar
     * @throws CharacterNotFoundException Si el ID no existe
     */
    @DeleteMapping("{userid}/characters/{characterid}")
    public ResponseEntity<Void> deleteCharacter(@PathVariable("userid") String userId, @PathVariable("characterid") String characterId) throws CharacterNotFoundException {
        characterService.deleteCharacter(characterId);
        return ResponseEntity.noContent().build();
    }

}

