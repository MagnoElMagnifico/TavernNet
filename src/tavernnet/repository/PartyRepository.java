package tavernnet.repository;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import tavernnet.model.Party;

import java.util.Set;

@Repository
public interface PartyRepository extends MongoRepository<Party, ObjectId> {
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'dm': ?1 } }")
    void updateDm(ObjectId partyId, String dm);

    @Query("{ '_id': ?0 }")
    @Update("{ '$addToSet': { 'members': { $each: ?1 } } }")
    void addMembers(ObjectId partyId, Set<ObjectId> members);

    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'members': ?1 } }")
    long removeMember(ObjectId partyId, ObjectId member);

    @Query("{ $or: [{ 'name': /?0/i }, {'description': /?0/i }] }")
    Page<Party> searchParties(String search, Pageable page);
}
