package tavernnet.service;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import tavernnet.exception.InvalidCredentialsException;
import tavernnet.exception.LimitException;
import tavernnet.model.Message;
import tavernnet.repository.CharacterRepository;
import tavernnet.repository.UserRepository;
import tavernnet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Party;
import tavernnet.model.User;
import tavernnet.model.Character;
import tavernnet.repository.PartyRepository;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@NullMarked
public class PartyService {

    private static final Logger log = LoggerFactory.getLogger(PartyService.class);

    private final PartyRepository partyRepo;
    private final CharacterRepository charRepo;
    private final UserRepository userRepo;

    @Autowired
    public PartyService(
        PartyRepository partyRepo,
        CharacterRepository charRepo,
        UserRepository userRepo
    ) {
        this.partyRepo = partyRepo;
        this.charRepo = charRepo;
        this.userRepo = userRepo;
    }

    public PagedModel<EntityModel<Party.Summary>> searchParties(
        String search,
        int page,
        int count
    ) {
        log.info("GET /parties search={} page={} count={}", search, page, count);
        // TODO: completar operacion
        return null;
    }

    public ObjectId createParty(Party.CreationRequest newParty) throws InvalidCredentialsException, ResourceNotFoundException {
        User.AuthUser user = Utils.safeGetAuthUser();

        // Validar que los miembros iniciales existen
        if (newParty.inicialMembers() != null) {
            for (String memberId : newParty.inicialMembers()) {
                if (!ObjectId.isValid(memberId) || !charRepo.existsById(new ObjectId(memberId))) {
                    throw new ResourceNotFoundException("Character", memberId);
                }
            }
        }

        // Almacenar en la BD
        Party party = partyRepo.save(Party.fromRequest(newParty, user.username()));
        log.info("POST /parties new party=\"{}\"", party.getId());
        return party.getId();
    }

    public Party getParty(ObjectId partyId) throws ResourceNotFoundException {
        Party party = partyRepo
            .findById(partyId)
            .orElseThrow(() -> new ResourceNotFoundException("Party", partyId.toHexString()));

        // Configurar detalles de los personajes miembros
        party.setMemberDetails(
            party
                .getMembersIds()
                .stream()
                .map(id -> {
                    Character character = charRepo.getCharacterById(id);
                    return Character.Summary.fromCharacter(character);
                })
                .toList()
        );

        return party;
    }

    public void deleteParty(ObjectId partyId) throws ResourceNotFoundException {
        if (!partyRepo.existsById(partyId)) {
            throw new ResourceNotFoundException("Party", partyId.toHexString());
        }
        partyRepo.deleteById(partyId);
    }

    // ==== EDITAR PARTY =======================================================

    public void changeDm(ObjectId partyId, Party.DmChangeRequest dm) throws ResourceNotFoundException {
        if (!partyRepo.existsById(partyId)) {
            throw new ResourceNotFoundException("Party", partyId.toHexString());
        }

        if (!userRepo.existsById(dm.username())) {
            throw new ResourceNotFoundException("User", dm.username());
        }

        partyRepo.updateDm(partyId, dm.username());
    }

    public void addMembers(ObjectId partyId, Set<ObjectId> newMembers) throws ResourceNotFoundException, LimitException {
        Party party = partyRepo
            .findById(partyId)
            .orElseThrow(() -> new ResourceNotFoundException("Party", partyId.toHexString()));

        // Limitar parties a 20 miembros
        if (party.getMembersIds().size() + newMembers.size() > 20) {
            throw new LimitException("Party member limit is 20");
        }

        for (ObjectId memberId : newMembers) {
            if (!charRepo.existsById(memberId)) {
                throw new ResourceNotFoundException("Character", memberId.toHexString());
            }
        }

        partyRepo.addMembers(partyId, newMembers);
    }

    public void deleteMember(ObjectId partyId, ObjectId member) throws ResourceNotFoundException {
        if (!partyRepo.existsById(partyId)) {
            throw new ResourceNotFoundException("Party", partyId.toHexString());
        }

        if (partyRepo.removeMember(partyId, member) != 1) {
            throw new ResourceNotFoundException("Character", member.toHexString());
        }
    }

    // ==== MENSAJES ===========================================================

    public PagedModel<EntityModel<Message>> getMessages(
        ObjectId partyId,
        LocalDateTime after,
        int page,
        int count
    ) {
        // TODO: implementar
        return null;
    }

    public void sendMessage(ObjectId partyId, Message.CreationRequest msg) {
        // TODO: implementar
    }
}
