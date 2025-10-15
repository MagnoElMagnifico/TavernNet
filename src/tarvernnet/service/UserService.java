package tarvernnet.service;

import java.util.HashSet;
import java.util.Set;
import tarvernnet.model.*;
import tarvernnet.repository.*;
import tarvernnet.exception.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private UserRepository userbase;

    @Autowired
    public UserService(UserRepository userbase) {
        this.userbase = userbase;
    }


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
        return userbase.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

}
