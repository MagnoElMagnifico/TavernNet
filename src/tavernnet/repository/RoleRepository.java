package tavernnet.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tavernnet.model.Role;

@Repository
@NullMarked
public interface RoleRepository extends MongoRepository<Role, String> {
}
