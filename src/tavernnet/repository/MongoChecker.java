package tavernnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

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
        } catch (Exception e) {
            log.error("Error checking MongoDB connection: {}", e.getMessage(), e);
        }
    }
}
