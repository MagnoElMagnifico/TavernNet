package tavernnet.repository;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tavernnet.model.*;
import tavernnet.model.Character;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

@Component
@NullMarked
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final MongoTemplate mongo;
    private final PasswordEncoder passwordEncoder;
    private final LikesRepository likesRepo;

    public DatabaseInitializer(MongoTemplate mongo, PasswordEncoder passwordEncoder, LikesRepository likesRepo) {
        this.mongo = mongo;
        this.passwordEncoder = passwordEncoder;
        this.likesRepo = likesRepo;
    }

    @Override
    public void run(String... args) {
        try {
            log.debug("Checking MongoDB...");

            String dbName = mongo.getDb().getName();
            log.info("Connected to MongoDB database: {}", dbName);

            var collections = mongo.getCollectionNames();
            if (!collections.isEmpty()) {
                log.info("Available collections: {}", mongo.getCollectionNames());
                return;
            }

            log.debug("Database appears to be empty, creating default collections...");
            createDefaultData();
            createIndices();
            createViews();
        } catch (Exception e) {
            log.error("Error checking MongoDB connection: {}", e.getMessage(), e);
        }
    }

    private void createDefaultData() {
        // ==== USUARIOS =======================================================

        User marcos = new User(
            "marcos",
            Objects.requireNonNull(passwordEncoder.encode("1234")),
            GlobalRole.ADMIN,
            LocalDateTime.now()
        );

        User jeremias = new User(
            "jeremias",
            Objects.requireNonNull(passwordEncoder.encode("password")),
            GlobalRole.USER,
            LocalDateTime.now()
        );

        mongo.insert(marcos);
        mongo.insert(jeremias);
        log.info("Created users \"marcos\" and \"jeremias\"");

        // ==== PERSONAJES =====================================================

        ObjectId zarionId = new ObjectId();
        Character zarion = Character.defaultCharacter(
            zarionId,
            "Zarion",
            "jeremias",
            "Esta es la biografia del personaje de Jeremias",
            "human",
            Arrays.asList("Common", "Draconic", "Elfic")
        );

        ObjectId eltonId = new ObjectId();
        Character.Stats eltonStats = new Character.Stats(8 , 13, 15, 12, 9);
        Character elton = new Character(
            eltonId,
            "Elton",
            "jeremias",
            "Esta es la biografia del segundo personaje de Jeremias",
            "elf",
            Arrays.asList("Common", "Elfic", "Undercommon", "Abisal"),
            LocalDateTime.now(),
            Character.Alignment.CHAOTIC_EVIL,
            eltonStats,
            Character.Stats.asModifiers(eltonStats),
            new Character.CombatStats(14, 32, 40, +3),
            new Character.PassiveStats(15),
            Character.Action.defaultActions()
        );

        mongo.insert(zarion);
        mongo.insert(elton);
        log.info("Created characters \"Zarion\" ({}) and \"Elton\" ({})", zarionId, eltonId);

        // ==== POSTS ==========================================================

        ObjectId postId1 = new ObjectId();
        Post post1 = new Post(
            postId1,
            zarionId,
            "Post de prueba",
            "Lorem ipsum dolor sit amet",
            LocalDateTime.of(2025, 10, 8, 8, 10, 30, 0)
        );

        ObjectId postId2 = new ObjectId();
        Post post2 = new Post(
            postId2,
            eltonId,
            "Post de prueba de otro personaje",
            "Lorem ipsum dolor sit amet",
            LocalDateTime.of(2025, 10, 9, 10, 38, 42, 0)
        );

        mongo.insert(post1);
        mongo.insert(post2);
        log.info("Created posts: {} {}", postId1, postId2);

        Comment comment = new Comment(
            new ObjectId(),
            "Muy buen post",
            postId1,
            eltonId,
            LocalDateTime.of(2025, 10, 8, 9, 16, 50, 0)
        );

        mongo.insert(comment);
        log.info("Created comment: {}", comment);

        likesRepo.addLike(postId1, eltonId);
        log.info("Added like to post {}", postId1);
    }

    private void createIndices() {
        // Acceso eficiente a personajes por nombre de usuario y nombre de personaje
        // Tampoco permite nombres de usuario y personajes repetidos
        mongo.indexOps("characters").createIndex(new Index()
            .on("user", Sort.Direction.ASC)
            .on("name", Sort.Direction.ASC)
            .unique()
        );
        log.info("Created characters index");

        // Solo 1 like por post
        mongo.indexOps("likes").createIndex(new Index()
            .on("post", Sort.Direction.ASC)
            .on("author", Sort.Direction.ASC)
            .unique()
        );
        log.info("Created likes index");
    }

    private void createViews() {
        // Crear una vista para comprobar fácilmente el número de comentarios y likes
        //
        // NOTAS SOBRE RENDIMIENTO
        // Crea una vista que se traducirá en consultas (no es materializada), por lo
        // que no es lo mejor cuando hay muchas consultas y documentos. Pero como esto
        // no es el objetivo de la práctica, así está lo suficientemente bien.
        //
        // Por otro lado, estas vistas son solo de lectura: no se pueden escribir en
        mongo.getDb().runCommand(Document.parse("""
            {
                create: "posts_view",
                viewOn: "posts",
                pipeline: [
                    { $lookup: { from: "likes", localField: "_id", foreignField: "post", as: "likes_docs" } },
                    { $lookup: { from: "comments", localField: "_id", foreignField: "post", as: "comments_docs" } },
                    { $addFields: {
                        n_likes: { $size: "$likes_docs" },
                        n_comments: { $size: "$comments_docs" }
                    }},
                    { $project: { likes_docs: 0, comments_docs: 0 } }
                ]
            }
        """));
        log.info("Created \"posts_view\" view");
    }
}
