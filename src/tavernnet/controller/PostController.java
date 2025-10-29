package tavernnet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import tavernnet.exception.PostNotFoundException;
import tavernnet.model.Post;
import tavernnet.repository.PostsRepository;
import tavernnet.service.PostService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
    // TODO: usar esto mejor: https://gitlab.citius.gal/docencia/es/rest-example/-/blob/error-management/src/main/java/gal/usc/etse/es/restdemo/controller/BookController.java?ref_type=tags#L58
    @PostMapping
    public ResponseEntity<Void> createPost(@RequestBody Post newPost) {
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
    public ResponseEntity<@Valid Post> getPost(@PathVariable("postid") String postId) {
        // TODO: Existe ResponseEntity.ofNullable(), que hace esto sin la excepción
        // TODO: Se puede hacer incluso sin pasar por el Service
        try {
            return ResponseEntity.ok(posts.getPost(postId));
        } catch (PostNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * <code>DELETE /posts/{postid}</code>
     * @param postId Identificador del post.
     * @return <code>204 No content</code> en éxito, <code>404 Not found</code>
     * si no existe el ID proporcionado.
     */
    @DeleteMapping("{postid}")
    public ResponseEntity<Void> deletePost(@PathVariable("postid") String postId) {
        try {
            posts.deletePost(postId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
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
     * <code>POST /posts/{postid}/comments</code>
     * @param postId ID del post en el que crear el comentario.
     * @param newComment Contenido del comentario.
     * @return <code>201 Created</code> en éxito, <code>404 Not found</code> si
     * no existe el ID proporcionado.
     *
    @PostMapping("{postid}/comments")
    public ResponseEntity<Void> createComment(
        @PathVariable("postid") String postId,
        @RequestBody @Valid Comment newComment
    ) {
    }
    */
}
