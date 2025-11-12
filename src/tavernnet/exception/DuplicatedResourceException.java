package tavernnet.exception;

public class DuplicatedResourceException extends Exception {
    private final Object otherResource;

    //Type of the element
    private final String type;
    // Search term used to find the element
    private final String id;

    public DuplicatedResourceException(Object otherResource, String type, String id) {
        super("%s %s already exists".formatted(type, id));
        this.otherResource = otherResource;
        this.type = type;
        this.id = id;
    }

    public Object getOtherResource() {
        return otherResource;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
