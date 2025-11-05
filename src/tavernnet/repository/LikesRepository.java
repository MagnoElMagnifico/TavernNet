package tavernnet.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/** Acceso a la coleccion <code>likes</code>. Como no existe una clase
 * <code>Like</code> en el modelo (no se usaria), se implementa de forma manual.
 */
@Repository
public class LikesRepository {
    private final MongoTemplate mongo;

    @Autowired
    public LikesRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void addLike(String postId, String authorId) {
        Document like = new Document();
        like.append("post", postId);
        like.append("author", authorId);
        mongo.insert(like, "likes");
    }

    public void removeLike(String postId, String authorId) {
        Query query = new Query(Criteria
            .where("post")
            .is(postId)
            .and("author")
            .is(authorId)
        );
        mongo.remove(query, "likes");
    }

    public void deleteByPostId(String postId) {
        Query query = new Query(Criteria.where("post").is(postId));
        mongo.remove(query, "likes");
    }

    public boolean existsLike(String postId, String authorId) {
        Query query = new Query(Criteria
            .where("post")
            .is(postId)
            .and("author")
            .is(authorId)
        );
        return mongo.exists(query, "likes");
    }
}
