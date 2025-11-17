package tavernnet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.Map;


import tavernnet.exception.*;
import tavernnet.model.*;
import tavernnet.repository.*;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import tavernnet.utils.PatchUtils;


@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final PatchUtils patchutils;
    private final UserRepository userbase;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userbase, PasswordEncoder passwordEncoder, PatchUtils patchutils) {
        this.userbase = userbase;
        this.passwordEncoder = passwordEncoder;
            this.patchutils = patchutils;
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

        Role userRole = null; // TODO: roles

        User user = new User(
            newUser.username(),
            passwordEncoder.encode(newUser.password()),
            userRole
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

    public User updateUser(String userId, List<Map<String, Object>> changes)
        throws ResourceNotFoundException, JsonPatchException {
        User user = userbase.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        User updated = patchutils.applyPatch(user, changes);
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

