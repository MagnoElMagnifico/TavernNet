package tavernnet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import tavernnet.exception.NotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.PostView;
import tavernnet.model.Post;
import tavernnet.repository.*;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostsRepository postsRepo;
    private final PostsViewRepository postsViewRepo;
    private final LikesRepository likesRepo;
    private final CommentsRepository commentRepo;
    private final CharacterRepository charRepo;

    @Autowired
    public PostService(
        PostsRepository postsRepo,
        PostsViewRepository postsViewRepo,
        CommentsRepository commentRepo,
        LikesRepository likesRepo,
        CharacterRepository charRepo
    ) {
        this.postsRepo = postsRepo;
        this.postsViewRepo = postsViewRepo;
        this.commentRepo = commentRepo;
        this.likesRepo = likesRepo;
        this.charRepo = charRepo;
    }

    /**
     * @return Lista de todos los posts.
     */
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    public List<PostView> getPosts() {
        return postsViewRepo.findAll();
    }

    /**
     * @param id Identificador del post.
     * @return El post que tiene el id especificado.
     * @throws NotFoundException Si el post no se encuentra.
     */
    public PostView getPost(String id) throws NotFoundException {
        return postsViewRepo.findById(id).orElseThrow(() -> new NotFoundException("Post", id));
    }

    /**
     * @param newPost Contenido del nuevo post a crear.
     * @return Id del nuevo post creado.
     */
    public String createPost(
        Post.UserInputPost newPost,
        String characterId
    ) throws NotFoundException {
        if (!charRepo.existsById(characterId)) {
            throw new NotFoundException("Character", characterId);
        }

        Post realPost = new Post(newPost, characterId);
        realPost = postsRepo.save(realPost);
        log.info("Created post with id '{}' by '{}'", realPost.getId(), characterId);

        return realPost.getId();
    }

    /**
     * @param postId Identificador del post a borrar
     * @throws NotFoundException Si el ID no existe
     */
    public void deletePost(String postId) throws NotFoundException {
        Post deletedPost = postsRepo.deletePostById(postId);
        if (deletedPost == null) {
            throw new NotFoundException("Post", postId);
        }

        // Borrar en cascada los elementos asociados al post
        commentRepo.deleteByPostId(postId);
        likesRepo.deleteByPostId(postId);
    }

    /**
     * @param postId Identificador del post a obtener sus comentarios
     * @return Lista de comentarios del post especificado
     * @throws NotFoundException Si el ID no existe
     */
    public List<Comment> getCommentsByPost(
        String postId
    ) throws NotFoundException {
        // Buscar si existe un post con este ID
        if (!postsRepo.existsById(postId)) {
            throw new NotFoundException("Post", postId);
        }

        // Obtener la lista de comentarios
        List<Comment> comments = commentRepo.getCommentsByPost(postId);
        if (comments == null) {
            throw new NotFoundException("Post", postId);
        }

        return comments;
    }

    /**
     * @param postId Identificador del post donde crear el comentario
     * @param newComment Datos del comentario a crear
     * @return Identificador del nuevo comentario
     * @throws NotFoundException Si el ID no existe
     */
    public String createComment(
        String postId,
        String characterId,
        Comment.UserInputComment newComment
    ) throws NotFoundException {
        // Comprobar si el post existe o no
        if (!postsRepo.existsById(postId)) {
            throw new NotFoundException("Post", postId);
        }

        if (!charRepo.existsById(characterId)) {
            throw new NotFoundException("Character", characterId);
        }

        Comment comment = new Comment(postId, characterId, newComment);
        comment = commentRepo.save(comment);

        log.info("Created comment in post '{}' by '{}'", postId, characterId);
        return comment.id();
    }

    public void giveLike(String postId, String characterId)
            throws NotFoundException {
        if (!postsRepo.existsById(postId)) {
            throw new NotFoundException("Post", postId);
        }

        if (!charRepo.existsById(characterId)) {
            throw new NotFoundException("Character", characterId);
        }

        likesRepo.addLike(postId, characterId);
        log.info("Character '{}' gave like to post '{}'", characterId, postId);
    }

    public void removeLike(String postId, String characterId)
            throws NotFoundException {
        if (!postsRepo.existsById(postId)) {
            throw new NotFoundException("Post", postId);
        }

        if (!charRepo.existsById(characterId)) {
            throw new NotFoundException("Character", characterId);
        }

        likesRepo.removeLike(postId, characterId);
        log.info("Character '{}' removed like to post '{}'", characterId, postId);
    }
}
