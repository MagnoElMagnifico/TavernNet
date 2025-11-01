package tavernnet.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import tavernnet.model.Post;

@Repository
public interface PostsRepository extends MongoRepository<@NotNull Post, @NotBlank String> {
    /**
     * @return Lista de todos los posts en la base de datos.
     */
    @Query("{}")
    List<@NotNull @Valid Post> getPosts();

    /**
     * @param id Id del post del que obtener los datos.
     * @return Post que tiene el ID dato o <code>null</code> si no existe.
     */
    @Query("{ '_id': '?0' }")
    @Valid Post getPostById(@NotBlank String id);

    @Query(value = "{ '_id': '?0' }", delete = true)
    Post deletePostById(String id);

    /**
     * @param post Guarda el post en la base de datos.
     * @return Devuelve el objeto que se almacenó en la base de datos.
     */
    <S extends @NotNull Post> S save(@Valid S post);
}
