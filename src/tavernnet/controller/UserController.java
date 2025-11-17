package tavernnet.controller;

import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
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
    private final UserService user;

    @Autowired
    public UserController(UserService user) {
        this.user = user;
    }

    // Servicio para obtener todos los usuarios
    @GetMapping()
    public @NonNull Collection<User.@Valid PublicProfile> getUsers() {
        return user.getUsers();
    }

    // Servicio para obtener un usuario por ID
    @GetMapping("{userid}")
    public @Valid User.PublicProfile getUser(
        @PathVariable("userid") @NotBlank String id
    ) throws ResourceNotFoundException {
        return user.getUser(id);
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
        user.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // Servicio para crear un nuevo usuario
    @PostMapping
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

    @PatchMapping("{userid}")
    public ResponseEntity<@Valid User> updateBook(
        @PathVariable("userid") String userId,
        @RequestBody List<Map<String, Object>> changes
    ) throws ResourceNotFoundException, JsonPatchException {
        return ResponseEntity.ok(userService.updateUser(userId, changes));
    }

}
