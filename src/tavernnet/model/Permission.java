package tavernnet.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// TODO: validacion
@Document(collection = "permissions")
public record Permission (
    @Id String id,
    String resource,
    String action
) {
    @Override
    public String toString() {
        return resource + ':' + action;
    }
}
