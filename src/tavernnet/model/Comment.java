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

/**
 * Representa un comentario sobre un post en concreto
 * @see PostView
 */
@Document(collection = "comments")
@NullMarked
public class Comment implements Ownable {

    // ==== DTOs ===============================================================

    /** DTO para crear un comentario */
    public record CommentRequest(
        // NOTA: el post viene especificado en la URL
        // NOTA: el autor viene especificado en la cabecera de autenticacion
        @NotBlank(message = "Comment content must be not blank")
        @Size(max = 1024, message = "Comment content maximum length is 1024 characters")
        String content
    ) {}

    // ==== ATRIBUTOS ==========================================================

    // Para que los ObjectId no se serialicen como objetos, pero se guarden como
    // tal en la BD, se ignoran al serializar a JSON y se añaden representaciones
    // en String para usar en el JSON, pero que también se ignoren en la BD.
    @Id
    @ValidObjectId(message = "Invalid comment id")
    @JsonIgnore
    private final ObjectId id;

    @ValidObjectId(message = "Invalid post id")
    @JsonIgnore
    private final ObjectId post;

    @ValidObjectId(message = "Invalid author id")
    @JsonIgnore
    private final ObjectId author;

    @ValidObjectId(message = "Invalid comment id")
    @Transient
    @JsonProperty("id")
    private final String idStr;

    @ValidObjectId(message = "Invalid comment post id")
    @Transient
    @JsonProperty("post")
    private final String postStr;

    @ValidObjectId(message = "Invalid comment author id")
    @Transient
    @JsonProperty("author")
    private final String authorStr;


    // Payload: copiados de lo que ha enviado el usuario
    @NotBlank(message = "Comment content must be not blank")
    @Size(max = 1024, message = "Comment content maximum length is 1024 characters")
    private final String content;


    // Configurado al crear un nuevo post
    @NotNull(message = "Comment date must be not null")
    private final LocalDateTime date;

    // ==== CONSTRUCTORES ======================================================

    public Comment(
        @ValidObjectId ObjectId id,
        @ValidObjectId ObjectId post,
        @ValidObjectId ObjectId author,
        @NotBlank String content,
        @NotBlank LocalDateTime date
    ) {
        this.id = id;
        this.idStr = id == null? null : id.toHexString();
        this.post = post;
        this.postStr = post.toHexString();
        this.author = author;
        this.authorStr = author.toHexString();
        this.content = content;
        this.date = date;
    }

    public Comment(
        @ValidObjectId ObjectId postId,
        @ValidObjectId ObjectId characterId,
        @Valid Comment.CommentRequest comment
    ) {
        this(null, postId, characterId, comment.content, LocalDateTime.now());
    }

    // ==== GETTERS ============================================================

    public ObjectId getId() {
        return id;
    }

    public ObjectId getPost() {
        return post;
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
    public String getOwnerId() {
        return authorStr;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Comment comment)) return false;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
