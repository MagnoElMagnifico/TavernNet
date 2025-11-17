package tavernnet.repository;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

import tavernnet.model.Comment;

@Repository
@NullMarked
public interface CommentsRepository extends MongoRepository<Comment, ObjectId> {
    /**
     * Obtiene todos los comentarios de un post concreto.
     * @param postId ID del post al que pertenecen los comentarios.
     */
    @Query("{ '_id.post': ?0 }")
    Optional<Collection<Comment>> getCommentsByPost(ObjectId postId);

    @Query(value = "{ 'id_post': ?0 }", delete = true)
    void deleteByPostId(ObjectId postId);

    /**
     * @param comment Guarda el nuevo comentario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull Comment> S save(S comment);
}
