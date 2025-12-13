package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import tavernnet.utils.ValidObjectId;

import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection = "posts")
@NullMarked
public class Post implements Ownable {

    // ==== DTOs ===============================================================

    /** DTO para crear un post */
    public record PostRequest(
        @NotBlank(message = "Title must be not null or blank")
        @Size(max = 64, message = "Post title maximum length is 64 characters")
        String title,

        @NotBlank(message = "Content must be not null or blank")
        @Size(max = 1024, message = "Post content maximum length is 1024 characters")
        String content
    ) {}

    // ==== ATRIBUTOS ==========================================================

    // Para que los ObjectId no se serialicen como objetos, pero se guarden como
    // tal en la BD, se ignoran al serializar a JSON y se añaden representaciones
    // en String para usar en el JSON, pero que también se ignoren en la BD.
    @Id
    @ValidObjectId(message = "Invalid post id")
    @JsonIgnore
    private ObjectId id;

    @ValidObjectId(message = "Invalid author id")
    @JsonIgnore
    private final ObjectId author;

    @ValidObjectId(message = "Invalid post id")
    @JsonProperty("id")
    @Transient
    private final String idStr;

    @ValidObjectId(message = "Invalid author id")
    @JsonProperty("author")
    @Transient
    private final String authorStr;


    // Payload: copiados de lo que ha enviado el usuario
    @NotBlank(message = "Title must be not null or blank")
    @Size(max = 64, message = "Post title maximum length is 64 characters")
    private final String title;

    @NotBlank(message = "Content must be not null or blank")
    @Size(max = 1024, message = "Post content maximum length is 1024 characters")
    private final String content;

    // TODO: liked by current user (extraer de la sesion)

    // Configurado al crear un nuevo post
    @NotNull(message = "Date must be not null")
    private final LocalDateTime date;

    // ==== CONSTRUCTORES ======================================================

    public Post(
        @ValidObjectId ObjectId id,
        @ValidObjectId ObjectId author,
        @NotBlank String title,
        String content,
        @Valid LocalDateTime date
    ) {
        this.id = id;
        this.idStr = id == null? null : id.toHexString();
        this.author = author;
        this.authorStr = author.toHexString();
        this.title = title;
        this.content = content;
        this.date = date;
    }

    /** Publicacion de un post por el usuario */
    public Post(@Valid Post.PostRequest post, @ValidObjectId ObjectId author) {
        // Dejar el ID a null hará que la base de datos asigne uno automáticamente
        this(null, author, post.title, post.content, LocalDateTime.now());
    }

    // ==== GETTERS ============================================================

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

    // ==== OTROS MÉTODOS ======================================================

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Post post)) return false;
        return Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String getOwnerId() {
        return authorStr;
    }
}
