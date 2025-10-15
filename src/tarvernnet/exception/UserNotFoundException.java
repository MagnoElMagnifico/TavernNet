package tarvernnet.exception;

public class UserNotFoundException extends Throwable{
    private final String id;

    public UserNotFoundException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
