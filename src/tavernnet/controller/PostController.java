package tavernnet.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.PostNotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.Post;
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
    public List<@Valid Post> getPosts() {
        return posts.getPosts();
    }

    /**
     * <code>POST /posts</code>
     * @param newPost Nueva publicación.
     * @return <code>201 Created</code> en éxito.
     */
    @PostMapping
    public ResponseEntity<Void> createPost(@RequestBody @Valid Post.UserInputPost newPost) {
        String newId = posts.createPost(newPost);

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
    public @Valid Post getPost(
        @PathVariable("postid") @NotBlank String postId
    ) throws PostNotFoundException {
        return posts.getPost(postId);
    }

    /**
     * <code>DELETE /posts/{postid}</code>
     * @param postId Identificador del post.
     * @return <code>204 No content</code> en éxito, <code>404 Not found</code>
     * si no existe el ID proporcionado.
     */
    @DeleteMapping("{postid}")
    public ResponseEntity<Void> deletePost(
        @PathVariable("postid") @NotBlank String postId
    ) throws PostNotFoundException {
        posts.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    // TODO: operación PATCH en los posts: dar likes
    /*
    @PatchMapping(path = "{postid}", consumes = "application/json-patch+json")
    @Valid
    public ResponseEntity updatePost(
        @PathVariable("postid") String postId,
        @RequestBody JsonPatch patch
    ) {
    }
    */

    /**
     * <code>GET /posts/{postid}/comments</code>
     * @param postId ID del post del que obtener los comentarios.
     * @return <code>200 OK</code> en éxito, <code>404 Not found</code> si
     * no existe el ID proporcionado.
     */
    @GetMapping("{postid}/comments")
    public List<Comment> getCommentsByPost(
        @PathVariable("postid") @NotBlank String postId
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
        @RequestBody @Valid Comment.UserInputComment newComment
    ) throws PostNotFoundException {
        Comment.CommentId newId = posts.createComment(postId, newComment);

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
