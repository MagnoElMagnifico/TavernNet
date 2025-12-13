package tavernnet.repository;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tavernnet.model.PostView;

import java.util.List;

/** Acceso a la vista <code>posts_view</code>. No se permiten operaciones de escritura */
@Repository
@NullMarked
public interface PostsViewRepository extends MongoRepository<PostView, ObjectId> {

    @Aggregation(pipeline = {
        // coincidencias en el título
        "{ $match: { 'title': /?0/, 'author': ?1 } }",
        "{ $sort: { 'creation': 1 } }",
        // coincidencias en el contenido
        """
        {
            $unionWith: {
                'coll': 'posts_view',
                'pipeline': [
                    { $match: { 'content': /?0/, 'author': ?1 } },
                    { $sort: { 'creation': 1 } }
                ]
            }
        }
        """,
        // eliminar duplicados
        "{ $group: { '_id': '$_id' } }",
        // elementos de paginación y número de resultados
        """
        {
            $facet: {
                'total_count': [{ $count: 'count' }],
                'page_data': [ {$skip: ?2}, {$limit: ?3} ]
            }
        }
        """
    })
    List<PostView> searchPosts(String search, String author, int page, int count);

}
