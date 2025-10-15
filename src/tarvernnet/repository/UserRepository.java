package tarvernnet.repository;
import tarvernnet.model.User;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;


public interface UserRepository extends MongoRepository<@NonNull User, @NonNull String>{
    User findByUserId(@NonNull String id);

    @Query("{'username':  '?0'}")
    Set<User> findByUsername(@NonNull String username);

    <S extends @NonNull User> S save(S user);
}
