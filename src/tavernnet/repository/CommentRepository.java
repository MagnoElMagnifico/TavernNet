package tavernnet.repository;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

import tavernnet.model.Comment;

@Repository
@NullMarked
public interface CommentRepository extends MongoRepository<Comment, ObjectId> {
    /**
     * Obtiene todos los comentarios de un post concreto.
     * @param postId ID del post al que pertenecen los comentarios.
     */
    @Query("{ 'post': ?0 }")
    Optional<Page<Comment>> getCommentsByPost(ObjectId postId, Pageable page);

    @Query(value = "{ 'post': ?0 }", delete = true)
    void deleteByPostId(ObjectId postId);

    @Aggregation(pipeline = {
        "{ $match: { 'post': ?0 } }",
        "{ $sort: { 'creation': -1 }}",
        "{ $limit: ?1 }"
    })
    Collection<Comment> getLatestComments(ObjectId postId, int number);

    /**
     * @param comment Guarda el nuevo comentario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull Comment> S save(S comment);
}
