package tavernnet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tavernnet.model.Party;

public interface PartyRepository extends MongoRepository<Party, String> {

    @Override
    boolean existsById(String s);
}
