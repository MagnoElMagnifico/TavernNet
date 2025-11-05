package tavernnet.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import tavernnet.model.Comment;

@Repository
public interface CommentsRepository extends MongoRepository<Comment, String> {
    /**
     * Obtiene todos los comentarios de un post concreto.
     * @param postId ID del post al que pertenecen los comentarios.
     */
    @Query("{ '_id.post': ?0 }")
    List<Comment> getCommentsByPost(String postId);

    @Query(value = "{ 'id_post': ?0 }", delete = true)
    void deleteByPostId(String postId);

    /**
     * @param comment Guarda el nuevo comentario en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull Comment> S save(S comment);
}
