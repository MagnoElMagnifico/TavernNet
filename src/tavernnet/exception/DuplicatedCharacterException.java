package tavernnet.exception;

import tavernnet.model.Character;

public class DuplicatedCharacterException extends Throwable{
    private final Character character;

    public DuplicatedCharacterException(Character character) {
        this.character = character;
    }

    public Character getUser() {
        return character;
    }
}
