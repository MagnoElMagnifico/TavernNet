package tarvernnet.exception;
import tarvernnet.model.User;

public class DuplicatedUserException extends Throwable{
    private final User user;

    public DuplicatedUserException(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
