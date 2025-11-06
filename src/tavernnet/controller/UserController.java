package tavernnet.controller;

import jakarta.validation.Valid;
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

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
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
    public @NonNull Set<@Valid User> getUsers() {
        return userService.getUsers();
    }

    // Servicio para obtener un usuario por ID
    @GetMapping("{userid}")
    public @Valid User getUser(@PathVariable("userid") String id) throws ResourceNotFoundException {
        return userService.getUser(id);
    }

    // Servicio para crear un nuevo usuario
    @PostMapping
    public ResponseEntity<@Valid User> addUser(@RequestBody User user) throws DuplicatedResourceException {
        String newId = userService.createUser(user);
        var url = MvcUriComponentsBuilder.fromMethodName(
                UserController.class,
                "getUser",
                newId)
            .build()
            .toUri();
        return ResponseEntity.created(url).body(user);
    }
}
