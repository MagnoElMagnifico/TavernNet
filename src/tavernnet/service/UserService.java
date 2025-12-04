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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

import tavernnet.exception.*;
import tavernnet.model.*;
import tavernnet.model.Character;
import tavernnet.repository.*;

@Service
@NullMarked
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final CharacterRepository charRepo;
    private final UserRefreshTokenRepository userRefreshRepo;
    private final RefreshTokenRepository refreshRepo;

    @Autowired
    public UserService(
        PasswordEncoder passwordEncoder,
        UserRepository userRepo,
        CharacterRepository charRepo,
        UserRefreshTokenRepository userRefreshRepo,
        RefreshTokenRepository refreshRepo
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.charRepo = charRepo;
        this.userRefreshRepo = userRefreshRepo;
        this.refreshRepo = refreshRepo;
    }

    public Pagination<String> getUsers(String searchTerm, int pageNumber, int pageSize) {
        log.debug("GET /users search={} page={} count={}", searchTerm, pageNumber, pageSize);
        var root = userRepo.searchByUsernameWithCount(
            Pattern.quote(searchTerm),
             pageNumber*pageSize,
             pageSize
        );
        return Pagination.from(root, pageNumber);
    }

    public User.PublicProfile getUser(String username) throws ResourceNotFoundException {
        User user = userRepo
            .findById(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));

        // Obtener los personajes de este usuario
        Collection<Character> characters = charRepo.getCharactersByUser(username);
        log.debug("GET /users/{} with {} characters", username, characters.size());

        return new User.PublicProfile(user, characters);
    }

    public void createUser(
        User.LoginRequest newUser
    ) throws DuplicatedResourceException {
        if (userRepo.existsById(newUser.username())) {
            throw new DuplicatedResourceException(newUser, "User", newUser.username());
        }

        User user = new User(
            newUser.username(),
            Objects.requireNonNull(passwordEncoder.encode(newUser.password())),
            GlobalRole.USER,
            LocalDateTime.now()
        );

        userRepo.save(user);
        log.debug("POST /users new user=\"{}\"", newUser.username());
    }

    /**
     * @param username Identificador del post a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public void deleteUser(String username) throws ResourceNotFoundException {
        userRepo
            .deleteUserById(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
        log.debug("DELETE /users/{} deleted user", username);

        // También borrar la sesión del usuario para que no queden sesiones "zombie"
        var optionalUrt = userRefreshRepo.findById(username);
        optionalUrt.ifPresent(urt -> {
            // Borrar tokens
            refreshRepo.deleteById(urt.uuid());
            // Borrar la entrada del usuario
            userRefreshRepo.deleteById(urt.username());
        });
        log.debug("DELETE /users/{} deleted user's refresh tokens", username);
    }

    // Necesario para que Spring sepa como obtener usuarios de la BD
    @Override
    public UserDetails loadUserByUsername(
        String username
    ) throws UsernameNotFoundException {
        log.debug("Load user=\"{}\"", username);
        return userRepo
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
