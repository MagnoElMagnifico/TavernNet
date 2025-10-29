package tavernnet.exception;

import jakarta.validation.constraints.NotBlank;

public class PostNotFoundException extends Throwable {
    private final String id;

    public PostNotFoundException(@NotBlank String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
