package tavernnet.exception;

public class DuplicatedResourceException extends Exception {
    private final Object otherResource;

    public DuplicatedResourceException(Object otherResource) {
        this.otherResource = otherResource;
    }

    public Object getOtherResource() {
        return otherResource;
    }
}
