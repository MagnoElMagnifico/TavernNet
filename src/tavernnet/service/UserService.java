package tavernnet.service;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

import tavernnet.exception.*;
import tavernnet.model.*;
import tavernnet.repository.*;

@Service
public class UserService {
    private final UserRepository userbase;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    public UserService(UserRepository userbase) {
        this.userbase = userbase;
    }

    /**
     * @param newUser Contenido del nuevo usuario a crear.
     * @return Id del nuevo usuario creado.
     */
    public String createUser(User newUser) throws DuplicatedUserException {

        if (!userbase.existsById(newUser.id())) {
            newUser = userbase.save(newUser);
            log.info("Created user with id '{}'", newUser.id());
            return newUser.id();
        } else {
            throw new DuplicatedUserException(newUser);
        }
    }

    public Set<User> getUsers() {
        return new HashSet<>(userbase.findAll());
    }

    public User getUser(@NonNull String id) throws UserNotFoundException {
        return userbase.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

}
