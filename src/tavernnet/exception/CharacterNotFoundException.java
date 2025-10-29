package tavernnet.exception;

import jakarta.validation.constraints.NotBlank;

public class CharacterNotFoundException extends Throwable {
    private final String id;

    public CharacterNotFoundException(@NotBlank String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
