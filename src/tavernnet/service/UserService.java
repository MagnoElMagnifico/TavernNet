package tavernnet.service;

import tavernnet.utils.patch.JsonPatchOperationType;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import tavernnet.exception.*;
import tavernnet.model.*;
import tavernnet.repository.*;
import tavernnet.utils.patch.JsonPatch;
import tavernnet.utils.patch.JsonPatchOperation;


@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userbase;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;

    @Autowired
    public UserService(UserRepository userbase, PasswordEncoder passwordEncoder, ObjectMapper mapper) {
        this.userbase = userbase;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    // TODO: paginacion
    // TODO: quiza esto no tiene demasiado sentido, mejor filtrar por nombre de usuario
    public Collection<User.PublicProfile> getUsers() {
        return userbase
            .findAll()
            .stream().
            map(u -> new User.PublicProfile(u, null))
            .toList();
    }

    public User.PublicProfile getUser(String id) throws ResourceNotFoundException {
        User user = userbase
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        // TODO: obtener personajes del usuario
        return new User.PublicProfile(user, null);
    }

    public void createUser(
        User.LoginRequest newUser
    ) throws DuplicatedResourceException {
        if (userbase.existsById(newUser.username())) {
            throw new DuplicatedResourceException(newUser, "User", newUser.username());
        }

        User user = new User(
            newUser.username(),
            passwordEncoder.encode(newUser.password()),
            GlobalRole.USER,
            LocalDateTime.now()
        );

        userbase.save(user);
        log.info("Created user with id '{}'", newUser.username());
    }

    /**
     * @param userId Identificador del post a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public void deleteUser(String userId) throws ResourceNotFoundException {
        userbase
            .deleteUserById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public User updateUser(String userId, List<JsonPatchOperation> changes)
        throws ResourceNotFoundException, JsonPatchFailedException {
        for(JsonPatchOperation operation: changes){
            if( (operation.operation() != JsonPatchOperationType.REPLACE)
                || operation.path().toString().equals("/password") )
                throw new JsonPatchFailedException(
                    "Operation %s on %s %s forbidden".formatted(
                        operation.operation().toString(), "User", userId));
        }

        User user = userbase.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        JsonNode updated_node = JsonPatch.apply(changes, mapper.convertValue(user, JsonNode.class));
        User updated = mapper.convertValue(updated_node, User.class);
        return userbase.save(updated);
    }

    // Necesario para que Spring sepa como obtener usuarios de la BD
    @Override
    @NullMarked
    public UserDetails loadUserByUsername(
        String username
    ) throws UsernameNotFoundException {
        return userbase
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}

