package tavernnet.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import tavernnet.exception.DuplicatedResourceException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Party;
import tavernnet.service.CharacterService;
import tavernnet.service.PartyService;
import tavernnet.service.UserService;
import tavernnet.utils.patch.JsonPatchOperation;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("parties")
public class PartyController {
    private static final Logger log = LoggerFactory.getLogger(PartyController.class);
    private final PartyService partyService;

    @Autowired
    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    /**
     * @return lista de parties existentes
     */
    @GetMapping
    public Collection<Party> getParties(){
        return partyService.getParties();
    }

    @PostMapping
    public ResponseEntity<Void> createParty(@RequestBody Party party)
        throws DuplicatedResourceException {
        var url = MvcUriComponentsBuilder.fromMethodName(
                CharacterController.class,
                "getCharacter",
                party.getName())
            .build()
            .toUri();

        return ResponseEntity.created(url).build();
    }

    @GetMapping("{party}")
    public Party getParty(@PathVariable("party") String party)
        throws ResourceNotFoundException {
        return partyService.getParty(party);
    }

    @PatchMapping("{party}")
    public ResponseEntity<@Valid Party> updateParty(
        @PathVariable("party") String partyId,
        @RequestBody List<JsonPatchOperation> changes)
        throws ResourceNotFoundException, JsonPatchFailedException {
        return ResponseEntity.ok(partyService.updateParty(partyId, changes));
    }

    @DeleteMapping("{party}")
    public ResponseEntity<Void> deleteParty(
        @PathVariable("party")
        @NotBlank(message = "Missing partyId to retrieve")
        String partyId
    ) throws ResourceNotFoundException {
        partyService.deleteParty(partyId);
        return ResponseEntity.noContent().build();
    }

}
