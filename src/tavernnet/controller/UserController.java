package tavernnet.controller;
import tavernnet.exception.*;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.jspecify.annotations.NonNull;

import tavernnet.exception.UserNotFoundException;
import tavernnet.model.User;
import tavernnet.service.CharacterService;
import tavernnet.service.UserService;

@RestController
@RequestMapping("users")
public class UserController {
    UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Servicio para obtener todos los usuarios
    @GetMapping()
    public ResponseEntity<@NonNull Set<User>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }


    // Servicio para obtener un usuario por ID
    @GetMapping("{userid}")
    public ResponseEntity<@NonNull User> getUser(@PathVariable("userid") String id) {
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
    }

}
