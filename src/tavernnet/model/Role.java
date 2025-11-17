package tavernnet.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

// TODO: validacion
@Document(collection = "roles")
public record Role(
    @Id String roleName,
    Set<Role> includes,
    Set<Permission> permissions
) {}
