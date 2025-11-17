package tavernnet.controller;

import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.User;
import tavernnet.service.UserService;
import com.github.fge.jsonpatch.JsonPatchOperation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    public @NonNull Collection<@Valid User> getUsers() {
        return userService.getUsers();
    }

    // Servicio para obtener un usuario por ID
    @GetMapping("{userid}")
    public User getUser(
        @PathVariable("userid")
        //@NotBlank(message = "Missing userId to retrieve")
        String id
    ) throws ResourceNotFoundException {
        return userService.getUser(id);
    }

    /**
     * <code>DELETE /users/{userid}</code>
     * @param userId Identificador del usuario.
     * @return <code>204 No content</code> en éxito, <code>404 Not found</code>
     * si no existe el ID proporcionado.
     */
    // TODO: errores de permisos
    @DeleteMapping("{userid}")
    public ResponseEntity<Void> deleteUser(
        @PathVariable("userid")
        @NotBlank(message = "Missing userId to retrieve")
        String userId
    ) throws ResourceNotFoundException {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // Servicio para crear un nuevo usuario
    @PostMapping
    public ResponseEntity<@Valid User> addUser(@RequestBody User user
    ) throws DuplicatedResourceException {
        String newId = userService.createUser(user);
        var url = MvcUriComponentsBuilder.fromMethodName(
                UserController.class,
                "getUser",
                newId)
            .build()
            .toUri();
        return ResponseEntity.created(url).body(user);
    }

    @PatchMapping("{userid}")
    public ResponseEntity<@Valid User> updateBook(
        @PathVariable("userid") String userId,
        @RequestBody List<Map<String, Object>> changes
    ) throws ResourceNotFoundException, JsonPatchException {
        return ResponseEntity.ok(userService.updateUser(userId, changes));
    }

}
