package tavernnet.service;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
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
    public String createUser(User newUser) throws DuplicatedResourceException {

        if (!userbase.existsById(newUser.id())) {
            newUser = userbase.save(newUser);
            log.info("Created user with id '{}'", newUser.id());
            return newUser.id();
        } else {
            throw new DuplicatedResourceException(newUser, "User", newUser.id());
        }
    }

    /**
     * @param userId Identificador del post a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public void deleteUser(String userId) throws ResourceNotFoundException {
        User deletedUser = userbase.deleteUserById(userId);
        if (deletedUser == null) {
            throw new ResourceNotFoundException("User", userId);
        }

    }

    public Collection<User> getUsers() {
        return userbase.findAll();
    }

    public User getUser(@NonNull String userId) throws ResourceNotFoundException {
        return userbase.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

}
