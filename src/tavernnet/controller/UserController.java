package tavernnet.controller;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.DuplicatedUserException;
import tavernnet.exception.UserNotFoundException;
import tavernnet.model.User;
import tavernnet.service.UserService;

import java.util.Set;

@RestController
@RequestMapping("users")
public class UserController {
    UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

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

        try {
            String newId = userService.createUser(user);
            var url = MvcUriComponentsBuilder.fromMethodName(
                    UserController.class,
                    "getUser",
                    newId)
                .build()
                .toUri();
            return ResponseEntity.created(url).body(user);
        }catch(DuplicatedUserException e){
            log.info("Usuario duplicado :P");
            return ResponseEntity.notFound().build();
        }
    }



}
