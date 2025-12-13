package tavernnet.service;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import tavernnet.exception.InvalidCredentialsException;
import tavernnet.exception.NoCharacterSelectedException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.PostView;
import tavernnet.model.Post;
import tavernnet.model.User;
import tavernnet.repository.*;
import tavernnet.utils.Utils;

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
    public List<PostView> getPosts(
        String search,
        String author,
        int page,
        int count
    ) {
        log.debug("GET /posts?search={}&author={}&page={}&count={}", search, author, page, count);
        return postsViewRepo.searchPosts(search, author, page, count);
    }

    /**
     * @param id Identificador del post.
     * @return El post que tiene el id especificado.
     * @throws ResourceNotFoundException Si el post no se encuentra.
     */
    public PostView getPost(ObjectId id) throws ResourceNotFoundException {
        String idStr = id.toHexString();
        log.debug("GET /posts/{}", idStr);
        return postsViewRepo
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", idStr));
    }

    /**
     * @param newPost Contenido del nuevo post a crear.
     * @return Id del nuevo post creado.
     */
    public ObjectId createPost(Post.PostRequest newPost) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException {
        User.AuthUser user = Utils.getAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", user.activeCharacter().toHexString());
        }

        Post realPost = new Post(newPost, user.activeCharacter());
        realPost = postsRepo.save(realPost);
        log.info("Created post with id '{}' by '{}'", realPost.getId(), user.activeCharacter());

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
    public List<Comment> getCommentsByPost(ObjectId postId) throws ResourceNotFoundException {
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
        Comment.CommentRequest newComment
    ) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException {
        User.AuthUser user = Utils.getAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        // Comprobar si el post existe o no
        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", String.valueOf(postId));
        }

        Comment comment = new Comment(postId, user.activeCharacter(), newComment);
        comment = commentRepo.save(comment);

        log.info("Created comment in post '{}' by '{}'", postId, user.activeCharacter().toHexString());
        return comment.getId();
    }

    public void giveLike(ObjectId postId) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException {
        User.AuthUser user = Utils.getAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", user.activeCharacter().toHexString());
        }

        likesRepo.addLike(postId, user.activeCharacter());
        log.info("Character '{}' gave like to post '{}'", user.activeCharacter().toHexString(), postId);
    }

    public void removeLike(ObjectId postId) throws ResourceNotFoundException, NoCharacterSelectedException, InvalidCredentialsException {
        User.AuthUser user = Utils.getAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", user.activeCharacter().toHexString());
        }

        likesRepo.removeLike(postId, user.activeCharacter());
        log.info("Character '{}' removed like to post '{}'", user.activeCharacter().toHexString(), postId);
    }
}
