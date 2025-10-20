package tarvernnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.bson.Document;

import java.util.List;

@Component
public class MongoChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoChecker.class);
    private final MongoTemplate mongoTemplate;

    public MongoChecker(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            String dbName = mongoTemplate.getDb().getName();
            log.info("Connected to MongoDB database: {}", dbName);

            var collections = mongoTemplate.getCollectionNames();
            log.info("Available collections: {}", collections);

            if (collections.contains("posts")) {
                long count = mongoTemplate.getCollection("posts").countDocuments();
                log.info("Collection 'posts' contains {} documents", count);

                Document firstDoc = mongoTemplate.getCollection("posts").find().first();
                if (firstDoc != null) {
                    log.debug("Example document from 'posts': {}", firstDoc.toJson());
                } else {
                    log.warn("Collection 'posts' is empty!");
                }
            } else {
                log.warn("Collection 'posts' does not exist in database '{}'", dbName);
            }

        } catch (Exception e) {
            log.error("Error checking MongoDB connection: {}", e.getMessage(), e);
        }
    }
}
