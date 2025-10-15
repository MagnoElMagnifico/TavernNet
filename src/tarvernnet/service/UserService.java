package tarvernnet.service;

import java.util.HashSet;
import java.util.Set;
import tarvernnet.model.*;
import tarvernnet.repository.*;
import tarvernnet.exception.*;
import org.jspecify.annotations.NonNull;

public class UserService {
    private UserRepository userbase;

    public User addUser(@NonNull User user) throws DuplicatedUserException {
        
        if (!userbase.existsById(user.id())) {
            return userbase.save(user);
        } else {
            throw new DuplicatedUserException(user);
        }
    }

    public Set<User> getUsers() {
        return new HashSet<>(userbase.findAll());
    }

    public User getUser(@NonNull String id) throws UserNotFoundException {
        if (userbase.existsById(id)) {
            return userbase.findByUserId(id);
        } else {
            throw new UserNotFoundException(id);
        }
    }

}
