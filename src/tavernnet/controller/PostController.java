package tavernnet.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.CharacterNotFoundException;
import tavernnet.exception.PostNotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.Post;
import tavernnet.model.PostView;
import tavernnet.service.PostService;

import java.util.List;

@RestController
@RequestMapping("posts")
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
    // TODO: parámetros para personalizar el algoritmo
    // TODO: paginación
    @GetMapping
    public List<@Valid PostView> getPosts() {
        return posts.getPosts();
    }

    /**
     * <code>POST /posts</code>
     * @param newPost Nueva publicación.
     * @return <code>201 Created</code> en éxito.
     */
    @PostMapping
    public ResponseEntity<Void> createPost(
        @RequestBody @Valid Post.UserInputPost newPost,
        // TODO: borrar cuando se implemente autenticacion
        @RequestParam(value = "author", required = true)
        @NotBlank(message = "Missing character id author of the post")
        String characterId
    ) throws CharacterNotFoundException {
        String newId = posts.createPost(newPost, characterId);

        var url = MvcUriComponentsBuilder.fromMethodName(
                PostController.class,
                "getPost",
                newId)
            .build()
            .toUri();

        return ResponseEntity.created(url).build();
    }

    /**
     * <code>GET /posts/{postid}</code>
     * @param postId Identificador del post.
     * @return <code>200 OK</code> con el post solicitado, <code>404 Not
     * found</code> si no existe el ID proporcionado.
     */
    @GetMapping("{postid}")
    public @Valid PostView getPost(
        @PathVariable("postid")
        @NotBlank(message = "Missing postId to retrieve")
        String postId
    ) throws PostNotFoundException {
        return posts.getPost(postId);
    }

    /**
     * <code>DELETE /posts/{postid}</code>
     * @param postId Identificador del post.
     * @return <code>204 No content</code> en éxito, <code>404 Not found</code>
     * si no existe el ID proporcionado.
     */
    // TODO: errores de permisos
    @DeleteMapping("{postid}")
    public ResponseEntity<Void> deletePost(
        @PathVariable("postid")
        @NotBlank(message = "Missing postId to retrieve")
        String postId
    ) throws PostNotFoundException {
        posts.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{postid}/like")
    public ResponseEntity<Void> giveLike(
        @PathVariable("postid") String postId,
        // TODO: borrar cuando se implemente autenticacion
        @RequestParam(value = "author", required = true)
        @NotBlank(message = "Missing character id author of the like")
        String characterId
    ) throws PostNotFoundException, CharacterNotFoundException {
        posts.giveLike(postId, characterId);

        var url = MvcUriComponentsBuilder.fromMethodName(
                PostController.class,
                "getPost",
                postId)
            .build()
            .toUri();

        return ResponseEntity.created(url).build();
    }

    // TODO: error de si el usuario no habia dado like antes
    @DeleteMapping("{postid}/like")
    public ResponseEntity<Void> removeLike(
        @PathVariable("postid") String postId,
        // TODO: borrar cuando se implemente autenticacion
        @RequestParam(value = "author", required = true)
        @NotBlank(message = "Missing character id author of the like")
        String characterId
    ) throws PostNotFoundException, CharacterNotFoundException {
        posts.removeLike(postId, characterId);
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
    public List<Comment> getCommentsByPost(
        @PathVariable("postid")
        @NotBlank(message = "Missing postId to retrieve comments from")
        String postId
    ) throws PostNotFoundException {
        return posts.getCommentsByPost(postId);
    }

    /**
     * <code>POST /posts/{postid}/comments</code>
     * @param postId ID del post en el que crear el comentario.
     * @param newComment Contenido del comentario.
     * @return <code>201 Created</code> en éxito, <code>404 Not found</code> si
     * no existe el ID proporcionado.
     */
    @PostMapping("{postid}/comments")
    public ResponseEntity<Void> createComment(
        @PathVariable("postid") String postId,
        @RequestBody @Valid Comment.UserInputComment newComment,
        // TODO: borrar cuando se implemente autenticacion
        @RequestParam(value = "author", required = true)
        @NotBlank(message = "Missing character id author of the comment")
        String characterId
    ) throws PostNotFoundException, CharacterNotFoundException {
        Comment.CommentId newId = posts.createComment(postId, characterId, newComment);

        // El enlace es a la lista de comentarios
        var url = MvcUriComponentsBuilder.fromMethodName(
                PostController.class,
                "getCommentsByPost",
                newId.post())
            .build()
            .toUri();

        return ResponseEntity.created(url).build();
    }
}
