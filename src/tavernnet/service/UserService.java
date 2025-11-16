package tavernnet.service;

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

import tavernnet.exception.*;
import tavernnet.model.*;
import tavernnet.repository.*;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userbase;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userbase, PasswordEncoder passwordEncoder) {
        this.userbase = userbase;
        this.passwordEncoder = passwordEncoder;
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
