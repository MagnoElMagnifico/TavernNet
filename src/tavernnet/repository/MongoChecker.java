package tavernnet.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoChecker.class);
    private final MongoTemplate mongo;

    public MongoChecker(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(String... args) {
        try {
            String dbName = mongo.getDb().getName();
            log.info("Connected to MongoDB database: {}", dbName);

            var collections = mongo.getCollectionNames();
            log.info("Available collections: {}", collections);
        } catch (Exception e) {
            log.error("Error checking MongoDB connection: {}", e.getMessage(), e);
        }
    }
}
