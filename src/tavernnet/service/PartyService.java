package tavernnet.service;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import tavernnet.exception.*;
import tavernnet.model.*;
import tavernnet.model.Character;
import tavernnet.model.Message;
import tavernnet.repository.CharacterRepository;
import tavernnet.repository.MessageRepository;
import tavernnet.repository.UserRepository;
import tavernnet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tavernnet.repository.PartyRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@NullMarked
public class PartyService {

    private static final Logger log = LoggerFactory.getLogger(PartyService.class);

    private final PartyRepository partyRepo;
    private final CharacterRepository charRepo;
    private final UserRepository userRepo;
    private final MessageRepository msgRepo;

    @Autowired
    public PartyService(
        PartyRepository partyRepo,
        CharacterRepository charRepo,
        UserRepository userRepo,
        MessageRepository msgRepo
    ) {
        this.partyRepo = partyRepo;
        this.charRepo = charRepo;
        this.userRepo = userRepo;
        this.msgRepo = msgRepo;
    }

    public Page<Party.Summary> searchParties(
        String search,
        int page,
        int count
    ) {
        log.debug("GET /parties search={} page={} count={}", search, page, count);
        return partyRepo
            .searchParties(search, PageRequest.of(page, count))
            .map(Party.Summary::fromParty);
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
        log.debug("POST /parties new party=\"{}\"", party.getId());
        return party.getId();
    }

    public Party getParty(ObjectId partyId) throws ResourceNotFoundException {
        Party party = partyRepo
            .findById(partyId)
            .orElseThrow(() -> new ResourceNotFoundException("Party", partyId.toHexString()));

        log.debug("GET /party/{} members={}", partyId, party.getMembersIds().size());

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
        log.debug("DELETE /party/{} party deleted", partyId);
        partyRepo.deleteById(partyId);
        log.debug("DELETE /party/{} party's messages deleted", partyId);
        msgRepo.deleteAllByParty(partyId);
    }

    // ==== EDITAR PARTY =======================================================

    public void changeDm(ObjectId partyId, Party.DmChangeRequest dm) throws ResourceNotFoundException {
        if (!partyRepo.existsById(partyId)) {
            throw new ResourceNotFoundException("Party", partyId.toHexString());
        }

        if (!userRepo.existsById(dm.username())) {
            throw new ResourceNotFoundException("User", dm.username());
        }

        log.debug("PUT /party/{}/dm DM updated to user=\"{}\"", partyId, dm.username());
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

        log.debug("POST /party/{}/members added new members={}", partyId, newMembers.size());
        partyRepo.addMembers(partyId, newMembers);
    }

    public void deleteMember(ObjectId partyId, ObjectId member) throws ResourceNotFoundException {
        if (!partyRepo.existsById(partyId)) {
            throw new ResourceNotFoundException("Party", partyId.toHexString());
        }

        if (partyRepo.removeMember(partyId, member) != 1) {
            throw new ResourceNotFoundException("Character", member.toHexString());
        }

        log.debug("DELETE /party/{}/members/{} deleted member", partyId, member);
    }

    // ==== MENSAJES ===========================================================

    public Slice<Message> getMessages(
        ObjectId partyId,
        String after,
        int count
    ) throws ResourceNotFoundException {
        if (!partyRepo.existsById(partyId)) {
            throw new ResourceNotFoundException("Party", partyId.toHexString());
        }

        // Obtener la slice de la BD
        Slice<Message> slice;
        var page = PageRequest.ofSize(count);
        if (after == null || after.isBlank()) {
            slice = msgRepo.getFirstSlice(partyId, page);
            log.debug("GET /party/{}/messages first slice after=\"{}\" count={}", partyId, after, count);
        } else {
            slice = msgRepo.getNextSlice(partyId, LocalDateTime.parse(after), page);
            log.debug("GET /party/{}/messages next slice after=\"{}\" count={}", partyId, after, count);
        }

        // Añadir detalles sobre el autor útiles para el cliente
        for (Message msg : slice) {
            if (msg.getAuthor() instanceof Message.CharacterAuthor charAuthor) {
                Character character = charRepo
                    .findById(charAuthor.characterId())
                    .orElseThrow(() -> new RuntimeException("Tried to set author details of invalid character for party member"));
                msg.setAuthorDetails(new Character.Summary(
                    character.getUser(),
                    character.getId().toHexString(),
                    character.getName(),
                    character.getLevel()
                ));
            }
        }

        return slice;
    }

    public void sendMessage(ObjectId partyId, Message.CreationRequest msg) throws ResourceNotFoundException, InvalidCredentialsException, NoCharacterSelectedException, InvalidMessageException {
        Party party = partyRepo
            .findById(partyId)
            .orElseThrow(() -> new ResourceNotFoundException("Party", partyId.toHexString()));

        if (msg.dice() == null && msg.text() == null) {
            throw new InvalidMessageException("Message cannot be empty: add text or dice roll");
        }

        User.AuthUser user = Utils.safeGetAuthUser();

        // Si no es DM y se intento usar un mensaje propio de un DM, error
        if (user.username().equals(party.getDm()))  {
            // En caso de que no hay autor, configurarlo para hablar como el DM
            if (msg.author() == null) {
                msg = new Message.CreationRequest(
                    Message.DmAuthor.asDm(),
                    msg.text(),
                    msg.dice()
                );
            }
        } else {
            // Si no es DM, no puede enviar un mensaje como tal
            if (msg.author() != null) {
                throw new AccessDeniedException("User \"%s\" is not DM: cannot send message as DM".formatted(user.username()));
            }

            // Si no es DM, el resto de usuarios deben actuar a traves de un personaje
            if (user.activeCharacter() == null) {
                throw new NoCharacterSelectedException();
            }
        }

        Message newMsg = Message.fromRequest(msg, user.activeCharacter(), partyId);
        newMsg = msgRepo.save(newMsg);
        log.debug(
            "POST /party/{}/messages id=\"{}\" text=\"{}\" roll=\"{}\" author=\"{}\"",
            partyId, newMsg.getId(), newMsg.getText(), newMsg.getRoll(), newMsg.getAuthor()
        );
    }
}
