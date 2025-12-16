package tavernnet.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.User;
import tavernnet.service.UserService;
import tavernnet.utils.Utils;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("users")
@ExposesResourceFor(User.class)
@NullMarked
public class UserController {
    private final UserService userService;
    private final EntityLinks entityLinks;

    @Autowired
    public UserController(UserService user, EntityLinks entityLinks) {
        this.userService = user;
        this.entityLinks = entityLinks;
    }

    // Servicio para obtener todos los usuarios.
    // Puede acceder tanto usuarios autenticados como no
    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @JsonView(User.class)
    @PreAuthorize("true")
    public ResponseEntity<PagedModel<User.PublicProfile>> getUsers(
        @RequestParam(value = "search", required = false, defaultValue = "")
        String searchTerm,

        @Min(value = 0, message = "Minimum page is 0")
        @RequestParam(value = "page", required = false, defaultValue = "0")
        int pageNumber,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 5, message = "Minimum page size is 5")
        @Max(value = 1000, message = "Maximum page size is 1000")
        int pageSize
    ) {
        var users = userService.getUsers(
            PageRequest.of(
                pageNumber,
                pageSize
            )
        );

        PagedModel<User.PublicProfile> response = PagedModel.of(
            users.getContent(), new PagedModel.PageMetadata(users.getSize(),
                users.getNumber(), users.getTotalElements(),
                users.getTotalPages()));

        // Links de hateoas

        response.add(linkTo(
            methodOn(UserController.class).getUsers(searchTerm, pageNumber, pageSize)
        ).withSelfRel());

        if(pageNumber < users.getTotalPages() - 1)
            response.add(linkTo(methodOn(UserController.class).getUsers(searchTerm,
            pageNumber + 1, pageSize)).withRel(IanaLinkRelations.NEXT));

        if(pageNumber > 0)
            response.add(linkTo(methodOn(UserController.class).getUsers(searchTerm,
            pageNumber - 1, pageSize)).withRel(IanaLinkRelations.PREVIOUS));

        response.add(linkTo(methodOn(UserController.class).getUsers(searchTerm,
            0, pageSize)).withRel(IanaLinkRelations.FIRST));

        response.add(linkTo(methodOn(UserController.class).getUsers(searchTerm,
            users.getTotalPages() - 1, pageSize)).withRel(IanaLinkRelations.LAST));

        return ResponseEntity.ok(response);
    }

    // Servicio para crear un nuevo usuario
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("true")
    public ResponseEntity<Void> addUser(
        @RequestBody @Valid User.LoginRequest request
    ) throws DuplicatedResourceException {
        userService.createUser(request);
        return ResponseEntity.created(Utils.getUrl("getUser", UserController.class, request.username())).build();
    }

    // Servicio para obtener un usuario por ID
    @GetMapping(
        path = "{userid}",
        produces = MediaTypes.HAL_JSON_VALUE
    )
    @JsonView(User.class)
    @PreAuthorize("true")
    public ResponseEntity<EntityModel<User.PublicProfile>> getUser(
        @PathVariable("userid") @NotBlank String id
    ) throws ResourceNotFoundException {
        EntityModel<User.PublicProfile> response = EntityModel.of(userService.getUser(id));
        response.add(entityLinks.linkToItemResource(User.class, id).withSelfRel());
        response.add(entityLinks.linkToCollectionResource(User.class).withRel(IanaLinkRelations.COLLECTION));
        return ResponseEntity.ok(response);
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
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
