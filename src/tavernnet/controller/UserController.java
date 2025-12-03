package tavernnet.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.User;
import tavernnet.service.UserService;

import java.util.Collection;

@RestController
@RequestMapping("users")
@NullMarked
public class UserController {
    private final UserService user;

    @Autowired
    public UserController(UserService user) {
        this.user = user;
    }

    // Servicio para obtener todos los usuarios.
    // Puede acceder tanto usuarios autenticados como no
    @GetMapping
    @PreAuthorize("true")
    public Collection<String> getUsers() {
        return user.getUsers();
    }

    // Servicio para obtener un usuario por ID
    @GetMapping("{userid}")
    @PreAuthorize("true")
    public User.PublicProfile getUser(
        @PathVariable("userid") @NotBlank String id
    ) throws ResourceNotFoundException {
        return user.getUser(id);
    }

    // Servicio para crear un nuevo usuario
    @PostMapping
    @PreAuthorize("true")
    public ResponseEntity<Void> addUser(
        @RequestBody @Valid User.LoginRequest request
    ) throws DuplicatedResourceException {
        user.createUser(request);
        var url = MvcUriComponentsBuilder.fromMethodName(
                UserController.class,
                "getUser",
                request.username())
            .build()
            .toUri();
        return ResponseEntity.created(url).build();
    }

    /**
     * <code>DELETE /users/{userid}</code>
     * @param username Identificador del usuario.
     * @return <code>204 No content</code> en éxito, <code>404 Not found</code>
     * si no existe el ID proporcionado.
     */
    @DeleteMapping("{userid}")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('users', #username, principal)")
    public ResponseEntity<Void> deleteUser(
        @PathVariable("userid")
        @NotBlank(message = "Missing username to retrieve")
        String username
    ) throws ResourceNotFoundException {
        user.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
