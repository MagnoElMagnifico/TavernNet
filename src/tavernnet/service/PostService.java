package tavernnet.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.InvalidCredentialsException;
import tavernnet.exception.NoCharacterSelectedException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Comment;
import tavernnet.model.PostView;
import tavernnet.model.Post;
import tavernnet.model.User;
import tavernnet.model.Character;
import tavernnet.repository.*;
import tavernnet.utils.Utils;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private static final int LATEST_COMMENTS = 10;

    private final PostsRepository postsRepo;
    private final PostsViewRepository postsViewRepo;
    private final LikesRepository likesRepo;
    private final CommentsRepository commentRepo;
    private final CharacterRepository charRepo;
    private final PagedResourcesAssembler<PostView> asmPost;
    private final PagedResourcesAssembler<Comment> asmComment;

    @Autowired
    public PostService(
        PostsRepository postsRepo,
        PostsViewRepository postsViewRepo,
        CommentsRepository commentRepo,
        LikesRepository likesRepo,
        CharacterRepository charRepo, PagedResourcesAssembler<PostView> asmPost, PagedResourcesAssembler<Comment> asmComment
    ) {
        this.postsRepo = postsRepo;
        this.postsViewRepo = postsViewRepo;
        this.commentRepo = commentRepo;
        this.likesRepo = likesRepo;
        this.charRepo = charRepo;
        this.asmPost = asmPost;
        this.asmComment = asmComment;
    }

    // ==== POSTS ==============================================================

    /**
     * @return Lista de todos los posts.
     */
    public Page<PostView> searchPosts(
        String search,
        String author,
        int page,
        int count
    ) {
        log.debug("GET /posts search={} author={} page={} count={}", search, author, page, count);

        // Crear un documento para filtrar
        Pattern pattern = search.isBlank()
            ? Pattern.compile(".*", Pattern.CASE_INSENSITIVE)
            : Pattern.compile(".*" + Pattern.quote(search) + ".*", Pattern.CASE_INSENSITIVE);

        Document match = new Document();

        List<Document> orList = new ArrayList<>();
        orList.add(new Document("title", pattern));
        orList.add(new Document("content", pattern));
        match.put("$or", orList);

        // filtro opcional por author
        if (author != null && !author.isBlank() && ObjectId.isValid(author)) {
            match.put("author", new ObjectId(author));
        }

        var root = postsViewRepo.searchPosts(match, page, count);
        return toPage(root, page, count);
    }

    /**
     * @param id Identificador del post.
     * @return El post que tiene el id especificado.
     * @throws ResourceNotFoundException Si el post no se encuentra.
     */
    public PostView getPost(ObjectId id) throws ResourceNotFoundException {
        String idStr = id.toHexString();
        log.debug("GET /posts/{}", idStr);

        // Realizar la consulta
        Document doc = postsViewRepo
            .getRawById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post", idStr));
        PostView post = getPostFromDoc(doc);

        // Completar con los datos que faltan
        User.AuthUser authUser = Utils.getAuthUser();
        ObjectId postAuthor = authUser == null? null : authUser.activeCharacter();
        completePost(post, postAuthor);

        return post;
    }

    /**
     * @param newPost Contenido del nuevo post a crear.
     * @return Id del nuevo post creado.
     */
    public ObjectId createPost(Post.PostRequest newPost) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException {
        User.AuthUser user = Utils.safeGetAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", user.activeCharacter().toHexString());
        }

        Post realPost = new Post(newPost, user.activeCharacter());
        realPost = postsRepo.save(realPost);
        log.debug("POST /posts/{} by character='{}'", realPost.getId(), user.activeCharacter());

        return realPost.getId();
    }

    /**
     * @param postId Identificador del post a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public void deletePost(ObjectId postId) throws ResourceNotFoundException {
        log.debug("DELETE /posts/{}", postId);
        postsRepo
            .deletePostById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", String.valueOf(postId)));

        // Borrar en cascada los elementos asociados al post
        commentRepo.deleteByPostId(postId);
        likesRepo.deleteByPostId(postId);

        log.debug("DELETE /posts/{} delete comments and likes of post", postId);
    }

    // ==== COMENTARIOS ========================================================

    /**
     * @param postId Identificador del post a obtener sus comentarios
     * @return Lista de comentarios del post especificado
     * @throws ResourceNotFoundException Si el ID no existe
     */
    public Page<Comment> getCommentsByPost(ObjectId postId, int page, int count) throws ResourceNotFoundException {
        // Buscar si existe un post con este ID
        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        // Obtener la lista de comentarios (paginado)
        var comments = commentRepo
            .getCommentsByPost(
                postId,
                // TODO: esto sale en el JSON por hateoas, pero no soportamos queries sort
                PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "creation"))
            )
            .orElseThrow(() -> new ResourceNotFoundException("Post", String.valueOf(postId)));

        // Añadir detalles del autor
        comments.forEach(this::setAuthorDetails);

        return comments;
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
        User.AuthUser user = Utils.safeGetAuthUser();
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

        log.debug("POST /posts/{}/comments new comment by character='{}'", postId, user.activeCharacter().toHexString());
        return comment.getId();
    }

    // ==== LIKES ==============================================================

    public void giveLike(ObjectId postId) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException, DuplicatedResourceException {
        User.AuthUser user = Utils.safeGetAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", user.activeCharacter().toHexString());
        }

        if (likesRepo.existsLike(postId, user.activeCharacter())) {
            throw new DuplicatedResourceException(null, "Like", postId.toHexString());
        }

        likesRepo.addLike(postId, user.activeCharacter());
        log.debug("POST /posts/{}/like character='{}' gave like to post", postId, user.activeCharacter().toHexString());
    }

    public void removeLike(ObjectId postId) throws ResourceNotFoundException, NoCharacterSelectedException, InvalidCredentialsException, DuplicatedResourceException {
        User.AuthUser user = Utils.safeGetAuthUser();
        if (user.activeCharacter() == null) {
            throw new NoCharacterSelectedException();
        }

        if (!postsRepo.existsById(postId)) {
            throw new ResourceNotFoundException("Post", String.valueOf(postId));
        }

        if (!charRepo.existsById(user.activeCharacter())) {
            throw new ResourceNotFoundException("Character", user.activeCharacter().toHexString());
        }

        if (!likesRepo.existsLike(postId, user.activeCharacter())) {
            // TODO: no es la excepcion mas apropiada para esto, pero por ahora sirve (debe devolver 409 Conflict)
            throw new DuplicatedResourceException(null, "Like", postId.toHexString());
        }

        likesRepo.removeLike(postId, user.activeCharacter());
        log.debug("DELETE /posts/{}/like character='{}' removed like to post", postId, user.activeCharacter().toHexString());
    }

    // ==== FUNCIONES DE AYUDA =================================================

    private PagedModel<EntityModel<PostView>> toPagedModel(AggregationResults<Document> root, int pageNumber, int pageSize) {
        var realRoot = root.getMappedResults().getFirst();

        long totalCount = 0;
        if (realRoot.get("total_count") instanceof List<?> list
            && !list.isEmpty()
            && list.getFirst() instanceof Map<?, ?> map
            && map.get("count") instanceof Number n) {
            totalCount = n.longValue();
        }

        // Para saber si el usuario actual le ha dado like, debemos saber qué usuario es
        User.AuthUser authUser = Utils.getAuthUser();
        ObjectId activeChar = authUser == null? null : authUser.activeCharacter();

        List<PostView> pageContent = new ArrayList<>();
        if (realRoot.get("page_data") instanceof List<?> pageData) {
            for (Object obj : pageData) {
                if (!(obj instanceof Document doc)) {
                    continue;
                }

                PostView post = getPostFromDoc(doc);
                completePost(post, activeChar);

                pageContent.add(post);
            }
        }

        return asmPost.toModel(
            new PageImpl<>(
                pageContent,
                PageRequest.of(pageNumber, pageSize),
                totalCount
            )
        );
    }

    private Page<PostView> toPage(AggregationResults<Document> root, int pageNumber, int pageSize) {
        var realRoot = root.getMappedResults().getFirst();

        long totalCount = 0;
        if (realRoot.get("total_count") instanceof List<?> list
            && !list.isEmpty()
            && list.getFirst() instanceof Map<?, ?> map
            && map.get("count") instanceof Number n) {
            totalCount = n.longValue();
        }

        // Para saber si el usuario actual le ha dado like, debemos saber qué usuario es
        User.AuthUser authUser = Utils.getAuthUser();
        ObjectId activeChar = authUser == null? null : authUser.activeCharacter();

        List<PostView> pageContent = new ArrayList<>();
        if (realRoot.get("page_data") instanceof List<?> pageData) {
            for (Object obj : pageData) {
                if (!(obj instanceof Document doc)) {
                    continue;
                }

                PostView post = getPostFromDoc(doc);
                completePost(post, activeChar);

                pageContent.add(post);
            }
        }

        return new PageImpl<>(
            pageContent,
            PageRequest.of(pageNumber, pageSize),
            totalCount
            );
    }



    // Para minimizar el número de llamadas a la API desde el frontend, se
    // añaden algunos datos extra.
    private void completePost(Post post, @Nullable ObjectId author) {
        // Más info sobre el autor
        Character character = charRepo
            .findById(post.getAuthor())
            .orElseThrow(() -> new RuntimeException("Tried to set author details of invalid character"));
        post.setAuthorDetails(
            character.getUser(),
            character.getName(),
            character.getLevel()
        );

        // Ver si el usuario actual ha dado like
        post.setLikedByCurrentUser(
            author == null
                ? null
                : likesRepo.existsLike(post.getId(), author)
        );

        // Completar con los primeros comentarios
        post.setLatestComments(
            commentRepo
                .getLatestComments(post.getId(), LATEST_COMMENTS)
                .stream()
                .peek(this::setAuthorDetails) // completar los detalles de cada comentario
                .toList()
        );
    }

    private void setAuthorDetails(Comment comment) {
        Character character = charRepo
            .findById(comment.getAuthor())
            .orElseThrow(() -> new RuntimeException("Tried to set author details of invalid character"));
        comment.setAuthorDetails(
            character.getUser(),
            character.getName(),
            character.getLevel()
        );
    }

    private PostView getPostFromDoc(Document doc) {
        // Como no hay forma de que Spring Data funcione bien con la herencia
        // entre Post y PostView (problemas con el campo _class), se crea
        // manualmente el objeto.
        return new PostView(
            doc.getObjectId("_id"),
            doc.getObjectId("author"),
            doc.getString("title"),
            doc.getString("content"),
            doc.getDate("creation")
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime(),
            doc.getInteger("likes"),
            doc.getInteger("comments")
        );
    }
}
