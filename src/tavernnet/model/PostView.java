package tavernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tavernnet.utils.ValidObjectId;

import java.time.LocalDateTime;

/**
 * Representa una publicación creada por un personaje determinado. Esta versión
 * incluye el número de likes y comentarios, lo que significa que está basada en
 * la vista.
 */
@Document(collection = "posts_view")
public record PostView (
    @Id
    @ValidObjectId(message = "Invalid post id")
    ObjectId id,

    @NotBlank(message = "Title must be not null or blank")
    String title,

    @NotBlank(message = "Content must be not null or blank")
    String content,

    // TODO: liked by current user (extraer de la sesion)

    @ValidObjectId(message = "Invalid author character id")
    ObjectId author,

    @NotNull(message = "Date must be not null")
    LocalDateTime date,

    @Field("n_likes")
    @Min(value = 0, message = "Likes must be a positive number")
    int nLikes,

    @Field("n_comments")
    @Min(value = 0, message = "Comments must be a positive number")
    int nComments
) implements Ownable {
    /** DTO para devolver datos de un post */
    public record PostResponse(
        @ValidObjectId(message = "Invalid post id")
        String id,

        @NotBlank(message = "Title must be not null or blank")
        String title,

        @ValidObjectId(message = "Invalid author character id")
        String author,

        @NotBlank(message = "Content must be not null or blank")
        String content,

        // TODO: liked by current user (extraer de la sesion)

        @NotNull(message = "Date must be not null")
        LocalDateTime date,

        @Min(value = 0, message = "Likes must be a positive number")
        int nLikes,

        @Min(value = 0, message = "Comments must be a positive number")
        int nComments
    ) {
        public PostResponse(@Valid PostView post) {
            this(
                post.id().toHexString(),
                post.title(),
                post.author().toHexString(),
                post.content(),
                post.date(),
                post.nLikes(),
                post.nComments()
            );
        }
    }

    @Override
    public String getOwnerId() {
        return author.toHexString();
    }
}

