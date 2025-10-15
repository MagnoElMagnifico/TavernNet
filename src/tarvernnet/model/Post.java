package tarvernnet.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;

@Document(collection = "posts")
public record Post (
    @Id
    ObjectId id,

    @NotBlank
    String title,

    @NotNull
    Date date,

    @NotEmpty
    ArrayList<Like> likes,

    @NotEmpty
    ArrayList<Comment> comments
) {}

