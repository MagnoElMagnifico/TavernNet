package tavernnet.repository;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tavernnet.model.Message;

import java.time.LocalDateTime;

@Repository
@NullMarked
public interface MessageRepository extends MongoRepository<Message, ObjectId> {
    // PAGINACIÓN MEDIANTE CURSORES
    // Primera página de mensajes
    @Query(
        value = "{ 'party': ?0 }",
        sort = "{ 'creation': -1, '_id': -1 }"
    )
    Slice<Message> getFirstSlice(ObjectId partyId, Pageable page);

    // Siguientes páginas
    @Query(
        value = "{ 'party': ?0, 'creation' : { $lt: ?1 } }",
        sort = "{ 'creation': -1, '_id': -1 }"
    )
    Slice<Message> getNextSlice(ObjectId partyId, LocalDateTime creation, Pageable page);
}
