package tavernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Representa un comentario sobre un post en concreto
 * @see Post
 */
@Document(collection = "comments")
public record Comment(
    @Valid
    @Id CommentId id,

    @NotBlank(message = "Comment content must be not blank")
    String content
) {
    /** Identificador de un comentario: post donde se publica, autor y fecha */
    public record CommentId(
        @NotBlank(message = "CommentId post must be not blank")
        String post,

        @NotBlank(message = "CommentId author must be not blank")
        String author,

        @NotNull(message = "CommentID date must be not null")
        LocalDateTime date
    ) {}

    /** Formato esperado del usuario al crear un comentario */
    public record UserInputComment(
        // NOTA: el post viene especificado en la URL

        @NotBlank(message = "Comment author must be not blank")
        String author,

        @NotBlank(message = "Comment content must be not blank")
        String content
    ) {}

    public Comment(@NotBlank String postId, @Valid UserInputComment comment) {
        this(
            new CommentId(
                postId,
                comment.author,
                LocalDateTime.now()
            ),
            comment.content
        );
    }
}
