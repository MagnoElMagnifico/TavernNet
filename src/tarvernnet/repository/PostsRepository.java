package tarvernnet.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.Query;
import org.bson.types.ObjectId;

import tarvernnet.model.Post;
import java.util.List;

@Repository
public interface PostsRepository extends MongoRepository<Post, String> {
    // @Query("{ }")
    // @Valid
    // public List<@NotNull Post> test();
}
