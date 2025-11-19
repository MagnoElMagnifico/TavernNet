package tavernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tavernnet.utils.ValidObjectId;

import java.time.LocalDateTime;

@Document(collection = "posts")
public class Post implements Ownable {
    /** DTO para crear un post */
    public record PostRequest(
        @NotBlank(message = "Title must be not null or blank")
        String title,

        @NotBlank(message = "Content must be not null or blank")
        String content
    ) {}

    // Datos internos
    @Id
    @ValidObjectId(message = "Invalid post id")
    private ObjectId id;

    @ValidObjectId(message = "Invalid author id")
    private final ObjectId author;

    @NotBlank(message = "Title must be not null or blank")
    private final String title;

    @NotBlank(message = "Content must be not null or blank")
    private final String content;

    // TODO: liked by current user (extraer de la sesion)

    @NotNull(message = "Date must be not null")
    private final LocalDateTime date;

    public Post(ObjectId id, ObjectId author, String title, String content, LocalDateTime date) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    /** Publicacion de un post por el usuario */
    public Post(@Valid Post.PostRequest post, @ValidObjectId ObjectId author) {
        // Dejar el ID a null hará que la base de datos asigne uno automáticamente
        this(null, author, post.title, post.content, LocalDateTime.now());
    }

    @Override
    public String getOwnerId() {
        return author.toHexString();
    }

    public void setId(@ValidObjectId ObjectId id) {
        this.id = id;
    }

    public ObjectId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ObjectId getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
