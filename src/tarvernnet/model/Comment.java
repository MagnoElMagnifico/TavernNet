package tarvernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "comments")
public record Comment(
    @Id @Valid CommentId id,
    @NotBlank String content
) {
    public record CommentId(
        @NotNull ObjectId post,
        @NotNull ObjectId author,
        @NotNull Date date
    ) {}
}
