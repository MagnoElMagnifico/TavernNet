package tavernnet.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tavernnet.model.Post;

@Repository
public interface PostsRepository
    extends MongoRepository<Post, ObjectId> {

    // No usar deleteById ya que ignora si no existe
    @Query(value = "{ '_id': ?0 }", delete = true)
    Post deletePostById(ObjectId id);
}
