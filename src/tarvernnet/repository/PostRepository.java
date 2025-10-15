package tarvernnet.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.bson.types.ObjectId;

import tarvernnet.model.Post;

@Repository
public interface PostRepository extends MongoRepository<@Valid Post, @NotNull ObjectId> {
}
