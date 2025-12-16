package tavernnet.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.InvalidCredentialsException;
import tavernnet.exception.NoCharacterSelectedException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.Post;
import tavernnet.model.PostView;
import tavernnet.service.PostService;
import tavernnet.utils.Utils;
import tavernnet.utils.ValidObjectId;

@RestController
@RequestMapping("posts")
@NullMarked
public class PostController {
    PostService posts;

    @Autowired
    public PostController(PostService posts) {
        this.posts = posts;
    }

    /**
     * <code>GET /posts</code>
     * @return <code>200 OK</code> con la lista de posts.
     */
    @GetMapping
    @PreAuthorize("true")
    public PagedModel<EntityModel<PostView>> getPosts(
        @RequestParam(value = "search", required = false, defaultValue = "")
        String search,

        @RequestParam(value = "author", required = false, defaultValue = "")
        String author,

        @RequestParam(value = "page", required = false, defaultValue = "0")
        @Min(value = 0, message = "Minimum page is 0")
        int page,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 1, message = "Minimum posts per page is 1")
        @Max(value = 100, message = "Maximum posts per page is 100")
        int count
    ) {
        return posts.searchPosts(search, author, page, count);
    }

    /**
     * <code>POST /posts</code>
     * @param newPost Nueva publicación.
     * @return <code>201 Created</code> en éxito.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated() and principal.activeCharacter != null")
    public ResponseEntity<Void> createPost(@RequestBody @Valid Post.PostRequest newPost) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException {
        ObjectId newId = posts.createPost(newPost);
        return ResponseEntity.created(Utils.getUrl("getPost", PostController.class, newId)).build();
    }

    /**
     * <code>GET /posts/{postid}</code>
     * @param postId Identificador del post.
     * @return <code>200 OK</code> con el post solicitado, <code>404 Not
     * found</code> si no existe el ID proporcionado.
     */
    @GetMapping("{postid}")
    @PreAuthorize("true")
    public PostView getPost(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid postId to retrieve")
        ObjectId postId
    ) throws ResourceNotFoundException {
        return posts.getPost(postId);
    }

    /**
     * <code>DELETE /posts/{postid}</code>
     * @param postId Identificador del post.
     * @return <code>204 No content</code> en éxito, <code>404 Not found</code>
     * si no existe el ID proporcionado.
     */
    @DeleteMapping("{postid}")
    @PreAuthorize("hasRole('ADMIN') or @auth.isCharOwner('posts', #postId, principal)")
    public ResponseEntity<Void> deletePost(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid postId to retrieve")
        ObjectId postId
    ) throws ResourceNotFoundException {
        posts.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{postid}/like")
    @PreAuthorize("isAuthenticated() and principal.activeCharacter != null")
    public ResponseEntity<Void> giveLike(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid post id")
        ObjectId postId
    ) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException, DuplicatedResourceException {
        posts.giveLike(postId);
        return ResponseEntity.created(Utils.getUrl("getPost", PostController.class, postId)).build();
    }

    // TODO: error de si el usuario no habia dado like antes
    @DeleteMapping("{postid}/like")
    @PreAuthorize("isAuthenticated() and principal.activeCharacter != null")
    public ResponseEntity<Void> removeLike(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid post id")
        ObjectId postId
    ) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException, DuplicatedResourceException {
        posts.removeLike(postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * <code>GET /posts/{postid}/comments</code>
     * @param postId ID del post del que obtener los comentarios.
     * @return <code>200 OK</code> en éxito, <code>404 Not found</code> si
     * no existe el ID proporcionado.
     */
    // TODO: paginacion si hay muchos comentarios
    @GetMapping("{postid}/comments")
    @PreAuthorize("true")
    public PagedModel<EntityModel<Comment>> getCommentsByPost(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid postId to retrieve comments from")
        ObjectId postId,

        @RequestParam(value = "page", required = false, defaultValue = "0")
        @Min(value = 0, message = "Minimum page is 0")
        int page,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 1, message = "Minimum posts per page is 1")
        @Max(value = 100, message = "Maximum posts per page is 100")
        int count
    ) throws ResourceNotFoundException {
        return posts.getCommentsByPost(postId, page, count);
    }

    /**
     * <code>POST /posts/{postid}/comments</code>
     * @param postId ID del post en el que crear el comentario.
     * @param newComment Contenido del comentario.
     * @return <code>201 Created</code> en éxito, <code>404 Not found</code> si
     * no existe el ID proporcionado.
     */
    @PostMapping("{postid}/comments")
    @PreAuthorize("isAuthenticated() and principal.activeCharacter != null")
    public ResponseEntity<Void> createComment(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid post id")
        ObjectId postId,
        @RequestBody @Valid
        Comment.CommentRequest newComment
    ) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException {
        ObjectId commentId = posts.createComment(postId, newComment);
        return ResponseEntity.created(Utils.getUrl("getCommentsByPost", PostController.class, postId, 0, 1)).build();
    }
}
