package tavernnet.repository;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tavernnet.model.PostView;

import java.util.Optional;

/** Acceso a la vista <code>posts_view</code>. No se permiten operaciones de escritura */
@Repository
@NullMarked
public interface PostsViewRepository extends MongoRepository<PostView, ObjectId> {

    @Aggregation(pipeline = {
        "{ $match: ?0 }",
        "{ $sort: { 'creation': 1 } }",
        // elementos de paginación y número de resultados
        """
        {
            $facet: {
                'total_count': [{ $count: 'count' }],
                'page_data': [ {$skip: ?1}, {$limit: ?2} ]
            }
        }
        """
    })
    AggregationResults<Document> searchPosts(Document match, int page, int count);

    // Spring Data no es capaz de crear objetos de PostView porque cree que son
    // Posts (campo _class). En su lugar, se crearan manualmente.
    Optional<Document> getRawById(ObjectId id);
}
