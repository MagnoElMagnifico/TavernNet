package tavernnet.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Character;
import tavernnet.model.Party;
import tavernnet.model.User;
import tavernnet.repository.PartyRepository;
import tavernnet.utils.patch.JsonPatch;
import tavernnet.utils.patch.JsonPatchOperation;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;

import java.util.Collection;
import java.util.List;

@Service
public class PartyService {
    private static final Logger log = LoggerFactory.getLogger(PartyService.class);
    private final PartyRepository parties;
    private final ObjectMapper mapper;

    public PartyService(PartyRepository parties, ObjectMapper mapper) {
        this.parties = parties;
        this.mapper = mapper;
    }

    /**
     * @return Lista de todas las parties.
     */
    public Collection<@NotNull @Valid Party> getParties() {
        return parties.findAll();
    }

    /**
     * @return Party especificada por id (nombre).
     */
    public @NotNull @Valid Party getParty(String partyId) throws ResourceNotFoundException {
        return parties.findById(partyId).orElseThrow(() -> new ResourceNotFoundException("Party", partyId));
    }

    /**
     * @param characters Personajes de la party a crear.
     * @param DM Dungeon Master de la party.
     * @param name Nombre e identificador de la party
     * @return party creada.
     */
    public Party createParty(Collection<Character> characters, User DM, String name) throws DuplicatedResourceException {
        // El nombre de la party debe ser nuevo
        if (parties.existsById(name)) {
            throw new DuplicatedResourceException(characters, "Party", name);
        }
        Party party = new Party(name, characters, DM);
        party = parties.save(party);
        log.info("Created party with id '{}'", party.getClass());
        return party;
    }

    /**
     *
     * @param partyId
     * @param changes Lista de cambios de la petición PATCH
     * @return party modificada
     */
    public Party updateParty(@NotBlank String partyId, List<JsonPatchOperation> changes)
        throws ResourceNotFoundException, JsonPatchFailedException {
        Party party = parties.findById(partyId).orElseThrow(
            () -> new ResourceNotFoundException("Party", partyId));
        JsonNode updated_node = JsonPatch.apply(
            changes, mapper.convertValue(party, JsonNode.class));
        Party updated = mapper.convertValue(updated_node, Party.class);
        return parties.save(updated);
    }

    public void deleteParty(@NotBlank String partyId)
        throws ResourceNotFoundException {
        Party deletedParty = parties.findById(partyId).orElseThrow(() -> new ResourceNotFoundException("Party", partyId));
        parties.deleteById(partyId);
    }

}
