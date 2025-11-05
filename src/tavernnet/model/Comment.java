package tavernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Representa un comentario sobre un post en concreto
 * @see PostView
 */
@Document(collection = "comments")
public record Comment(
    @Id String id,

    @NotBlank(message = "Comment content must be not blank")
    String content,

    @NotBlank(message = "CommentId post must be not blank")
    String post,

    @NotBlank(message = "CommentId author must be not blank")
    String author,

    @NotNull(message = "CommentID date must be not null")
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
        @NotBlank String postId,
        @NotBlank String characterId,
        @Valid UserInputComment comment
    ) {
        this(null, comment.content, postId, characterId, LocalDateTime.now());
    }
}
