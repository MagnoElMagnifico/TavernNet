package tavernnet.exception;

public class ResourceNotFoundException extends Exception {
    // Type of the element
    private final String type;

    // Search term used to find the element
    private final String id;

    public ResourceNotFoundException(String type, String id) {
        super("%s with id '%s' is not found".formatted(type, id));
        this.id = id;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
