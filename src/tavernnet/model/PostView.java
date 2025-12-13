package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Representa una publicación creada por un personaje determinado. Esta versión
 * incluye el número de likes y comentarios, lo que significa que está basada en
 * la vista (no se pueden crear directamente).
 */
@Document(collection = "posts_view")
public class PostView extends Post {

    // ==== ATRIBUTOS ==========================================================

    @Min(value = 0, message = "Likes must be a positive number")
    @Field("n_likes")
    @JsonProperty("n_likes")
    private final int nLikes;

    @Min(value = 0, message = "Comments must be a positive number")
    @Field("n_comments")
    @JsonProperty("n_comments")
    private final int nComments;

    // ==== CONSTRUCTORES ======================================================

    public PostView(
        ObjectId id,
        ObjectId author,
        String title,
        String content,
        LocalDateTime date,
        int nLikes,
        int nComments
    ) {
        super(id, author, title, content, date);
        this.nLikes = nLikes;
        this.nComments = nComments;
    }

    public PostView(Post post, int nLikes, int nComments) {
        this(
            post.getId(),
            post.getAuthor(),
            post.getTitle(),
            post.getContent(),
            post.getDate(),
            nLikes,
            nComments
        );
    }

    // ==== GETTERS ============================================================

    public int getLikes() {
        return nLikes;
    }

    public int getComments() {
        return nComments;
    }
}

