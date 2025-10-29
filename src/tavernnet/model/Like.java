package tavernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.bson.types.ObjectId;

@Document(collection = "likes")
public record Like(
    @Id @Valid LikeId id
) {
    public record LikeId(
        @NotNull ObjectId post,
        @NotNull ObjectId author
    ) {}
}
