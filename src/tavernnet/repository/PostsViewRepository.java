package tavernnet.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tavernnet.model.PostView;

/** Acceso a la vista <code>posts_view</code>. No se permiten operaciones de escritura */
@Repository
public interface PostsViewRepository
    extends MongoRepository<PostView, ObjectId> {
}
