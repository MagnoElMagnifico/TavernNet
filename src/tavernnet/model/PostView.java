package tavernnet.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Representa una publicación creada por un personaje determinado. Esta versión
 * incluye el número de likes y comentarios, lo que significa que está basada en
 * la vista.
 */
@Document(collection = "posts_view")
public record PostView (
    // Datos internos
    @Id
    String id,

    @NotBlank(message = "Title must be not null or blank")
    String title,

    @NotBlank(message = "Content must be not null or blank")
    String content,

    // TODO: id del character autor

    @NotNull(message = "Date must be not null")
    LocalDateTime date,

    @Field("n_likes")
    @Min(value = 0, message = "Likes must be a positive number")
    int nLikes,

    @Field("n_comments")
    @Min(value = 0, message = "Comments must be a positive number")
    int nComments
) {
}

