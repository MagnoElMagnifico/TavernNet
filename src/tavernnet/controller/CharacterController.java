package tavernnet.controller;

import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.NotFoundException;
import tavernnet.service.UserService;
import tavernnet.service.CharacterService;
import tavernnet.model.Character;

import java.util.List;

@RestController
@RequestMapping("users")
public class CharacterController {
    UserService userService;
    CharacterService characterService;

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
    public ResponseEntity<Void> createCharacter(@PathVariable("userid") String id, @RequestBody Character newCharacter) {
        String newId = characterService.createCharacter(newCharacter);

        var url = MvcUriComponentsBuilder.fromMethodName(
                CharacterController.class,
                "getCharacter",
                newId)
            .build()
            .toUri();

        return ResponseEntity.created(url).build();
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
    public @Valid Character getCharacter(@PathVariable("userid") String userId, @PathVariable("characterid") String characterId) throws NotFoundException {
        return characterService.getCharacter(characterId);
    }
    /*

    // Servicio para obtener un usuario por ID
    @GetMapping("{user-id}")
    public ResponseEntity<@NonNull User> getUser(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(userService.getUser(id));
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Servicio para crear un nuevo usuario
    @PostMapping
    public ResponseEntity<@NonNull User> addUser(@RequestBody User user) {
        try{
            user = userService.addUser(user);

            return ResponseEntity
                .created(MvcUriComponentsBuilder.fromMethodName(
                    UserController.class, "getUser",
                    user.id()).build().toUri())
                .body(user);
        } catch (DuplicatedResourceException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .location(MvcUriComponentsBuilder.fromMethodName(
                    UserController.class, "getUser",
                    user.id()).build().toUri())
                .build();
        }
    }*/

}

