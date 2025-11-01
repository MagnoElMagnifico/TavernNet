package tavernnet.service;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import tavernnet.exception.PostNotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.Post;
import tavernnet.repository.CommentsRepository;
import tavernnet.repository.PostsRepository;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private final PostsRepository postsRepo;
    private final CommentsRepository commentRepo;

    @Autowired
    public PostService(PostsRepository postsRepo, CommentsRepository commentRepo, Validator validator) {
        this.postsRepo = postsRepo;
        this.commentRepo = commentRepo;
    }

    /**
     * @return Lista de todos los posts.
     */
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    public @NotNull List<@Valid Post> getPosts() {
        return postsRepo.findAll();
    }

    /**
     * @param id Identificador del post.
     * @return El post que tiene el id especificado.
     * @throws PostNotFoundException Si el post no se encuentra.
     */
    public @Valid Post getPost(@NotBlank String id) throws PostNotFoundException {
        Post post = postsRepo.getPostById(id);
        if (post == null) {
            throw new PostNotFoundException(id);
        }
        return post;
    }

    /**
     * @param newPost Contenido del nuevo post a crear.
     * @return Id del nuevo post creado.
     */
    public String createPost(@Valid Post.UserInputPost newPost) {
        // TODO: añadir el personaje author (validar que exista)
        Post realPost = new Post(newPost);
        realPost = postsRepo.save(realPost);
        log.info("Created post with id '{}'", realPost.id());
        return realPost.id();
    }

    /**
     * @param postId Identificador del post a borrar
     * @throws PostNotFoundException Si el ID no existe
     */
    public void deletePost(@NotBlank String postId) throws PostNotFoundException {
        Post deletedPost = postsRepo.deletePostById(postId);
        if (deletedPost == null) {
            throw new PostNotFoundException(postId);
        }

        // Borrar en cascada los elementos asociados al post
        commentRepo.deleteByPostId(postId);
        // TODO: delete likes
    }

    /**
     * @param postId Identificador del post a obtener sus comentarios
     * @return Lista de comentarios del post especificado
     * @throws PostNotFoundException Si el ID no existe
     */
    public List<Comment> getCommentsByPost(
        @NotBlank String postId
    ) throws PostNotFoundException {
        // Buscar si existe un post con este ID
        if (!postsRepo.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }

        // Obtener la lista de comentarios
        List<Comment> comments = commentRepo.getCommentsByPost(postId);
        if (comments == null) {
            throw new PostNotFoundException(postId);
        }

        return comments;
    }

    /**
     * @param postId Identificador del post donde crear el comentario
     * @param newComment Datos del comentario a crear
     * @return Identificador del nuevo comentario
     * @throws PostNotFoundException Si el ID no existe
     */
    public @Valid Comment.CommentId createComment(
        @NotBlank String postId, @Valid Comment.UserInputComment newComment
    ) throws PostNotFoundException {
        // Comprobar si el post existe o no
        if (!postsRepo.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }

        // TODO: Comprobar si el personaje existe

        Comment comment = new Comment(postId, newComment);
        comment = commentRepo.save(comment);
        return comment.id();
    }
}

