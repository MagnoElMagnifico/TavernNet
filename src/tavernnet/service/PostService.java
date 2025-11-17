package tavernnet.service;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

import tavernnet.exception.ResourceNotFoundException;
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
    public Collection<PostView.PostResponse> getPosts() {
        return postsViewRepo.findAll().stream().map(PostView.PostResponse::new).toList();
    }

    /**
     * @param id Identificador del post.
     * @return El post que tiene el id especificado.
     * @throws ResourceNotFoundException Si el post no se encuentra.
     */
    public PostView.PostResponse getPost(ObjectId id) throws ResourceNotFoundException {
        return new PostView.PostResponse(postsViewRepo
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", String.valueOf(id)))
        );
    }

    /**
     * @param newPost Contenido del nuevo post a crear.
     * @return Id del nuevo post creado.
     */
    public ObjectId createPost(
        Post.PostRequest newPost,
        ObjectId characterId
    ) throws ResourceNotFoundException {
        if (!charRepo.existsById(characterId)) {
            throw new ResourceNotFoundException("Character", String.valueOf(characterId));
        }

        Post realPost = new Post(newPost, characterId);
        realPost = postsRepo.save(realPost);
        log.info("Created post with id '{}' by '{}'", realPost.getId(), characterId);

        return realPost.getId();
    }

    /**
     * @param postId Identificador del post a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public void deletePost(ObjectId postId) throws ResourceNotFoundException {
        postsRepo
            .deletePostById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", String.valueOf(postId)));

        // Borrar en cascada los elementos asociados al post
        commentRepo.deleteByPostId(postId);
        likesRepo.deleteByPostId(postId);
    }

    /**
     * @param postId Identificador del post a obtener sus comentarios
     * @return Lista de comentarios del post especificado
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public Collection<Comment> getCommentsByPost(
        ObjectId postId
    ) throws ResourceNotFoundException {
        // Buscar si existe un post con este ID
        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        // Obtener la lista de comentarios
        return commentRepo
            .getCommentsByPost(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", String.valueOf(postId)));
    }

    /**
     * @param postId Identificador del post donde crear el comentario
     * @param newComment Datos del comentario a crear
     * @return Identificador del nuevo comentario
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public ObjectId createComment(
        ObjectId postId,
        ObjectId characterId,
        Comment.CommentRequest newComment
    ) throws ResourceNotFoundException {
        // Comprobar si el post existe o no
        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(characterId)) {
            throw new ResourceNotFoundException("Character", String.valueOf(postId));
        }

        Comment comment = new Comment(postId, characterId, newComment);
        comment = commentRepo.save(comment);

        log.info("Created comment in post '{}' by '{}'", postId, characterId);
        return comment.id();
    }

    public void giveLike(ObjectId postId, ObjectId characterId)
            throws ResourceNotFoundException {
        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(characterId)) {
            throw new ResourceNotFoundException("Character", String.valueOf(characterId));
        }

        likesRepo.addLike(postId, characterId);
        log.info("Character '{}' gave like to post '{}'", characterId, postId);
    }

    public void removeLike(ObjectId postId, ObjectId characterId)
            throws ResourceNotFoundException {
        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(characterId)) {
            throw new ResourceNotFoundException("Character", String.valueOf(characterId));
        }

        likesRepo.removeLike(postId, characterId);
        log.info("Character '{}' removed like to post '{}'", characterId, postId);
    }
}
