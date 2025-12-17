package tavernnet.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
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
import tavernnet.model.User;
import tavernnet.service.PostService;
import tavernnet.utils.Utils;
import tavernnet.utils.ValidObjectId;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("posts")
@ExposesResourceFor(Post.class)
@NullMarked
public class PostController {
    PostService posts;
    private final EntityLinks entityLinks;

    @Autowired
    public PostController(PostService posts, EntityLinks entityLinks) {
        this.posts = posts;
        this.entityLinks = entityLinks;
    }

    /**
     * <code>GET /posts</code>
     * @return <code>200 OK</code> con la lista de posts.
     */
    @GetMapping
    @JsonView(PostView.class)
    @PreAuthorize("true")
    public ResponseEntity<PagedModel<PostView>> getPosts(
        @RequestParam(value = "search", required = false, defaultValue = "")
        String searchTerm,

        @RequestParam(value = "author", required = false, defaultValue = "")
        String author,

        @RequestParam(value = "page", required = false, defaultValue = "0")
        @Min(value = 0, message = "Minimum page is 0")
        int pageNumber,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 1, message = "Minimum posts per page is 1")
        @Max(value = 100, message = "Maximum posts per page is 100")
        int pageSize
    ) {
        var found_posts = posts.searchPosts(searchTerm, author, pageNumber, pageSize);

        PagedModel<PostView> response = PagedModel.of(
            found_posts.getContent(),
            new PagedModel.PageMetadata(found_posts.getSize(),
                found_posts.getNumber(),
                found_posts.getTotalElements(),
                found_posts.getTotalPages())
        );

        // Links de hateoas

        response.add(linkTo(
            methodOn(PostController.class).getPosts(searchTerm, author,
                pageNumber, pageSize)).withSelfRel());

        if(pageNumber < found_posts.getTotalPages() - 1)
            response.add(linkTo(methodOn(PostController.class).getPosts(searchTerm,
                author,pageNumber + 1,
                pageSize)).withRel(IanaLinkRelations.NEXT));

        if(pageNumber > 0)
            response.add(linkTo(methodOn(PostController.class).getPosts(searchTerm,
                author,pageNumber - 1,
                pageSize)).withRel(IanaLinkRelations.PREVIOUS));

        response.add(linkTo(methodOn(PostController.class).getPosts(searchTerm,
            author,0, pageSize)).withRel(IanaLinkRelations.FIRST));

        response.add(linkTo(methodOn(PostController.class).getPosts(searchTerm,
            author,found_posts.getTotalPages() - 1,
            pageSize)).withRel(IanaLinkRelations.LAST));

        return ResponseEntity.ok(response);
    }

    /**
     * <code>POST /posts</code>
     * @param newPost Nueva publicación.
     * @return <code>201 Created</code> en éxito.
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
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
    @GetMapping(
        path = "{postid}",
        produces = MediaTypes.HAL_JSON_VALUE)
    @JsonView(PostView.class)
    @PreAuthorize("true")
    public ResponseEntity<EntityModel<PostView>> getPost(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid postId to retrieve")
        ObjectId postId
    ) throws ResourceNotFoundException {
        EntityModel<PostView> response = EntityModel.of(posts.getPost(postId));

        // Links de hateoas
        response.add(entityLinks.linkToItemResource(Post.class, postId).withSelfRel());

        response.add(entityLinks.linkToCollectionResource(Post.class).withRel(
            IanaLinkRelations.COLLECTION));

        response.add(entityLinks.linkToItemResource(Post.class,
            response.getContent().getAuthor()).withRel(IanaLinkRelations.AUTHOR));

        response.add(linkTo(methodOn(PostController.class).getCommentsByPost(
            postId,0,10)).withRel("comments"));

        return ResponseEntity.ok(response);
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

    // ==== LIKES ==============================================================

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

    // ==== COMENTARIOS ========================================================

    /**
     * <code>GET /posts/{postid}/comments</code>
     * @param postId ID del post del que obtener los comentarios.
     * @return <code>200 OK</code> en éxito, <code>404 Not found</code> si
     * no existe el ID proporcionado.
     */
    // TODO: paginacion si hay muchos comentarios
    @GetMapping(
        path = "{postid}/comments",
        produces = MediaTypes.HAL_JSON_VALUE)
    @JsonView(Comment.class)
    @PreAuthorize("true")
    public ResponseEntity<PagedModel<Comment>> getCommentsByPost(
        @PathVariable("postid")
        @ValidObjectId(message = "Invalid postId to retrieve comments from")
        ObjectId postId,

        @RequestParam(value = "page", required = false, defaultValue = "0")
        @Min(value = 0, message = "Minimum page is 0")
        int pageNumber,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 1, message = "Minimum posts per page is 1")
        @Max(value = 100, message = "Maximum posts per page is 100")
        int pageSize
    ) throws ResourceNotFoundException {
        var found_comments = posts.getCommentsByPost(postId, pageNumber, pageSize);
        PagedModel<Comment> response = PagedModel.of(
            found_comments.getContent(),
            new PagedModel.PageMetadata(found_comments.getSize(),
                found_comments.getNumber(),
                found_comments.getTotalElements(),
                found_comments.getTotalPages())
        );

        // Links de hateoas

        response.add(linkTo(
            methodOn(PostController.class).getCommentsByPost(postId,
                pageNumber, pageSize)).withSelfRel());

        if(pageNumber < found_comments.getTotalPages() - 1)
            response.add(linkTo(methodOn(PostController.class).getCommentsByPost(postId,
                pageNumber + 1, pageSize)).withRel(IanaLinkRelations.NEXT));

        if(pageNumber > 0)
            response.add(linkTo(methodOn(PostController.class).getCommentsByPost(postId,
                pageNumber - 1, pageSize)).withRel(IanaLinkRelations.PREVIOUS));

        response.add(linkTo(methodOn(PostController.class).getCommentsByPost(postId,
            0, pageSize)).withRel(IanaLinkRelations.FIRST));

        response.add(linkTo(methodOn(PostController.class).getCommentsByPost(postId,
            found_comments.getTotalPages() - 1,
            pageSize)).withRel(IanaLinkRelations.LAST));

        return ResponseEntity.ok(response);
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
        posts.createComment(postId, newComment);
        return ResponseEntity.created(Utils.getUrl("getCommentsByPost", PostController.class, postId, 0, 1)).build();
    }
}
