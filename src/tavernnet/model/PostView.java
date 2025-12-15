package tavernnet.model;

import jakarta.validation.constraints.Min;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private final int likes;

    @Min(value = 0, message = "Comments must be a positive number")
    private final int comments;

    // ==== CONSTRUCTORES ======================================================

    public PostView(
        ObjectId id,
        ObjectId author,
        String title,
        String content,
        LocalDateTime date,
        int likes,
        int comments
    ) {
        super(id, author, title, content, date);
        this.likes = likes;
        this.comments = comments;
    }

    public PostView(Post post, int likes, int comments) {
        this(
            post.getId(),
            post.getAuthor(),
            post.getTitle(),
            post.getContent(),
            post.getCreation(),
            likes,
            comments
        );
    }

    // ==== GETTERS ============================================================

    public int getLikes() {
        return likes;
    }

    public int getComments() {
        return comments;
    }
}

