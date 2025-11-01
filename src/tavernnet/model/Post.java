package tavernnet.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Representa una publicacion creada por un personaje determinado */
@Document(collection = "posts")
public record Post (
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

    // TODO: esto se calcula desde el numero de likes
    @Min(value = 0, message = "Likes must be a positive number")
    int likes
) {
    /** Formato esperado del usuario al hacer POST para crear un post */
    public record UserInputPost (
        @NotBlank(message = "Title must be not null or blank")
        String title,

        @NotBlank(message = "Content must be not null or blank")
        String content

        // TODO: id del character autor
    ) {}

    /** Publicacion de un post por el usuario */
    public Post(Post.UserInputPost post) {
        // Dejar el ID a null hará que la base de datos asigne uno automáticamente
        this(null, post.title, post.content, LocalDateTime.now(), 0);
    }
}

