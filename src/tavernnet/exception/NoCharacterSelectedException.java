package tavernnet.exception;

public class NoCharacterSelectedException extends Exception {
    public NoCharacterSelectedException() {
        super("No character is selected: use /auth/character-login");
    }
}
