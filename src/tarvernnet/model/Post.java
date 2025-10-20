package tarvernnet.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "posts")
public class Post {
    @Id
    private final String id;

    @NotBlank
    private String title;

    @NotNull
    private LocalDateTime date;

    @Size(min = 0)
    private int likes;

    public Post(String id, String title, LocalDateTime date, int likes) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.likes = likes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(@NotNull LocalDateTime date) {
        this.date = date;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(@Size(min = 0) int likes) {
        this.likes = likes;
    }
}

