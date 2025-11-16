package tavernnet.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tavernnet.utils.ValidObjectId;

import java.time.LocalDateTime;

/**
 * Representa un comentario sobre un post en concreto
 * @see PostView
 */
@Document(collection = "comments")
public record Comment(
    @Id
    @ValidObjectId(message = "Invalid comment id")
    ObjectId id,

    @NotBlank(message = "Comment content must be not blank")
    String content,

    @ValidObjectId(message = "Invalid post id")
    ObjectId post,

    @ValidObjectId(message = "Invalid author id")
    ObjectId author,

    @NotNull(message = "Comment date must be not null")
    LocalDateTime date
) {
    /** DTO para crear un comentario */
    public record CommentRequest(
        // NOTA: el post viene especificado en la URL
        // NOTA: el autor viene especificado en la cabecera de autenticacion
        @NotBlank(message = "Comment content must be not blank")
        String content
    ) {}

    /** DTO para devolver un comentario */
    public record CommentResponse(
        @Id
        @ValidObjectId(message = "Invalid comment id")
        String id,

        @NotBlank(message = "Comment content must be not blank")
        String content,

        @ValidObjectId(message = "Invalid post id")
        String post,

        @ValidObjectId(message = "Invalid author id")
        String author,

        @NotNull(message = "Comment date must be not null")
        LocalDateTime date
    ) {
        public CommentResponse(Comment comment) {
            this(
                comment.id().toHexString(),
                comment.content(),
                comment.post().toHexString(),
                comment.author().toHexString(),
                comment.date()
            );
        }
    }

    public Comment(
        @ValidObjectId ObjectId postId,
        @ValidObjectId ObjectId characterId,
        @Valid Comment.CommentRequest comment
    ) {
        this(null, comment.content, postId, characterId, LocalDateTime.now());
    }
}
