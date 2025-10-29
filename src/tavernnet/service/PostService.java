package tavernnet.service;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tavernnet.exception.PostNotFoundException;
import tavernnet.model.Post;
import tavernnet.repository.PostsRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private final PostsRepository repo;
    private final Validator validator;

    @Autowired
    public PostService(PostsRepository repo, Validator validator) {
        this.repo = repo;
        this.validator = validator;
    }

    /**
     * @return Lista de todos los posts.
     */
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    public @NotNull List<@Valid Post> getPosts() {
        return repo.findAll();
    }

    /**
     * @param id Identificador del post.
     * @return El post que tiene el id especificado.
     * @throws PostNotFoundException Si el post no se encuentra.
     */
    public @NotNull @Valid Post getPost(@NotBlank String id) throws PostNotFoundException {
        Post post = repo.getPostById(id);
        if (post == null) {
            throw new PostNotFoundException(id);
        }
        return post;
    }

    /**
     * @param newPost Contenido del nuevo post a crear.
     * @return Id del nuevo post creado.
     */
    public String createPost(Post newPost) {
        // Likes debe ser 0
        newPost.setLikes(0);

        // La fecha tiene que ser la de ahora
        newPost.setDate(LocalDateTime.now());

        if (newPost.getId() != null) {
            // TODO: evitar esto de alguna forma
            throw new RuntimeException("Crear post con id establecido");
        }

        // Validar ahora el resto de campos: titulo y contenido
        var violations = validator.validate(newPost);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        newPost = repo.save(newPost);
        log.info("Created post with id '{}'", newPost.getId());
        return newPost.getId();
    }

    public void deletePost(@NotBlank String postId) {
        throw new RuntimeException("Not implemented");
    }
}

