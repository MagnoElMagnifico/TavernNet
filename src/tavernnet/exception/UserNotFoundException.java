package tavernnet.exception;

import jakarta.validation.constraints.NotBlank;

public class UserNotFoundException extends Throwable{
    private final String id;

    public UserNotFoundException(@NotBlank String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
