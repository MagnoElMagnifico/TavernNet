package tavernnet.repository;

import org.bson.Document;
import org.bson.types.ObjectId;
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
import java.util.List;
import java.util.Map;

@Component
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
            passwordEncoder.encode("1234"),
            GlobalRole.ADMIN,
            LocalDateTime.now()
        );

        User jeremias = new User(
            "jeremias",
            passwordEncoder.encode("password"),
            GlobalRole.USER,
            LocalDateTime.now()
        );

        mongo.insert(marcos);
        mongo.insert(jeremias);
        log.info("Created users \"marcos\" and \"jeremias\"");

        // ==== PERSONAJES =====================================================

        ObjectId zarionId = new ObjectId();
        Character zarion = new Character(
            zarionId,
            "Zarion",
            "jeremias",
            "Esta es la biografia del personaje de Jeremias",
            "human",
            Arrays.asList("Common", "Draconic", "Elfic"),
            "lawful good",
            new Stats(
                Map.of(
                    "CON", 10,
                    "STR", 10,
                    "DEX", 10,
                    "WIS", 10,
                    "CHA", 10
                ),
                Map.of(
                    "CON", 1,
                    "STR", 1,
                    "DEX", 1,
                    "WIS", 1,
                    "CHA", 1
                ),
                Map.of(
                    "perception", 12
                ),
                Map.of(
                    "HP", 8,
                    "AC", 12,
                    "speed", 30,
                    "iniciative", 2
                )
            ),
            List.of(),
            LocalDateTime.now()
        );

        ObjectId eltonId = new ObjectId();
        Character elton = new Character(
            eltonId,
            "Elton",
            "jeremias",
            "Esta es la biografia del segundo personaje de Jeremias",
            "elf",
            Arrays.asList("Common", "Elfic", "Undercommon", "Abisal"),
            "lawful good",
            new Stats(
                Map.of(
                    "CON", 8,
                    "STR", 13,
                    "DEX", 15,
                    "WIS", 12,
                    "CHA", 9
                ),
                Map.of(
                    "CON", -2,
                    "STR", 3,
                    "DEX", 4,
                    "WIS", 1,
                    "CHA", 0
                ),
                Map.of(
                    "perception", 14
                ),
                Map.of(
                    "HP", 15,
                    "AC", 13,
                    "speed", 40,
                    "iniciative", 5
                )
            ),
            List.of(),
            LocalDateTime.now()
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
