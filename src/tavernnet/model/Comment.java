package tavernnet.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tavernnet.utils.ObjectIdSerializer;
import tavernnet.utils.ValidObjectId;

import java.time.LocalDateTime;

/**
 * Representa un comentario sobre un post en concreto
 * @see PostView
 */
@Document(collection = "comments")
public record Comment(
    @Id
    @JsonSerialize(using = ObjectIdSerializer.class)
    @ValidObjectId(message = "Invalid comment id")
    ObjectId id,

    @NotBlank(message = "Comment content must be not blank")
    String content,

    @JsonSerialize(using = ObjectIdSerializer.class)
    @ValidObjectId(message = "Invalid post id")
    ObjectId post,

    @JsonSerialize(using = ObjectIdSerializer.class)
    @ValidObjectId(message = "Invalid author id")
    ObjectId author,

    @NotNull(message = "Comment date must be not null")
    @JsonSerialize(using = ObjectIdSerializer.class)
    LocalDateTime date
) {
    /** Formato esperado del usuario al crear un comentario */
    public record UserInputComment(
        // NOTA: el post viene especificado en la URL
        // NOTA: el autor viene especificado en la cabecera de autenticacion
        @NotBlank(message = "Comment content must be not blank")
        String content
    ) {}

    public Comment(
        @ValidObjectId ObjectId postId,
        @ValidObjectId ObjectId characterId,
        @Valid UserInputComment comment
    ) {
        this(null, comment.content, postId, characterId, LocalDateTime.now());
    }
}
