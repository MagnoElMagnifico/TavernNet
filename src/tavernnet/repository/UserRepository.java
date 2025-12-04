package tavernnet.repository;

import org.bson.Document;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import tavernnet.model.User;

@Repository
@NullMarked
public interface UserRepository extends MongoRepository<User, String> {

    @Aggregation(pipeline = {
        // el usuario que coincida entero primero
        "{ $match: { '_id': ?0 } }",
        "{ $project: { '_id': true } }",
        // luego unir con los que empiecen por ese trozo
        """
        {
            $unionWith: {
                'coll': 'users',
                'pipeline': [
                    { $match: { '_id': /^?0/ } },
                    { $project: { '_id': true } },
                    { $sort: { '_id': 1 } }
                ]
            }
        }
        """,
        // a continuación cualquier substring
        """
        {
            $unionWith: {
                'coll': 'users',
                'pipeline': [
                    { $match: { '_id': /?0/ } },
                    { $project: { '_id': true } },
                    { $sort: { '_id': 1 } }
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
                'page_data': [ {$skip: ?1}, {$limit: ?2} ]
            }
        }
        """
    })
    AggregationResults<Document> searchByUsernameWithCount(String regex, int skip, int limit);

    @Query("{'_id':  '?0'}")
    Optional<User> findByUsername(String username);

    // No usar deleteById ya que ignora si no existe
    @Query(value = "{ '_id': ?0 }", delete = true)
    Optional<User> deleteUserById(String id);

    /**
     * @param user Guarda el usuario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends User> S save(S user);
}
