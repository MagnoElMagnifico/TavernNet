package tavernnet.controller;

import org.jspecify.annotations.NonNull;
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

import tavernnet.exception.DuplicatedUserException;
import tavernnet.exception.UserNotFoundException;
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

    @Autowired
    public CharacterController(UserService userService, CharacterService characterService) {
        this.userService = userService;
        this.characterService = characterService;
    }

    // Servicio para obtener todos los personajes de un usuario
    @GetMapping("{userid}/characters")
    public ResponseEntity<@NonNull List<Character>> getCharacters(@PathVariable("userid") String id) {
        return ResponseEntity.ok(characterService.getCharactersByUser(id));
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
        } catch (DuplicatedUserException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .location(MvcUriComponentsBuilder.fromMethodName(
                    UserController.class, "getUser",
                    user.id()).build().toUri())
                .build();
        }
    }*/

}

