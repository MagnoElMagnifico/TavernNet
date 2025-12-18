package tavernnet.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tavernnet.exception.InvalidCredentialsException;
import tavernnet.exception.LimitException;
import tavernnet.exception.NoCharacterSelectedException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Message;
import tavernnet.model.Party;
import tavernnet.service.PartyService;
import tavernnet.utils.Utils;
import tavernnet.utils.ValidObjectId;

import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("parties")
@NullMarked
public class PartyController {

    private final PartyService partyService;

    @Autowired
    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @JsonView(Party.class)
    @PreAuthorize("true")
    public ResponseEntity<PagedModel<Party.Summary>> searchParties(
        @RequestParam(value = "search", required = false, defaultValue = "")
        String searchTerm,

        @RequestParam(value = "page", required = false, defaultValue = "0")
        @Min(value = 0, message = "Minimum page is 0")
        int pageNumber,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 1, message = "Minimum parties per page is 1")
        @Max(value = 100, message = "Maximum parties per page is 100")
        int pageSize
    ) {
        var found_parties = partyService.searchParties(searchTerm, pageNumber, pageSize);

        PagedModel<Party.Summary> response = PagedModel.of(
            found_parties.getContent(),
            new PagedModel.PageMetadata(found_parties.getSize(),
                found_parties.getNumber(),
                found_parties.getTotalElements(),
                found_parties.getTotalPages())
        );

        // Links de hateoas

        response.add(linkTo(
            methodOn(PartyController.class).searchParties(searchTerm, pageNumber, pageSize)
        ).withSelfRel());

        if(pageNumber < found_parties.getTotalPages() - 1)
            response.add(linkTo(methodOn(PartyController.class).searchParties(searchTerm,
                pageNumber + 1, pageSize)).withRel(IanaLinkRelations.NEXT));

        if(pageNumber > 0)
            response.add(linkTo(methodOn(PartyController.class).searchParties(searchTerm,
                pageNumber - 1, pageSize)).withRel(IanaLinkRelations.PREVIOUS));

        response.add(linkTo(methodOn(PartyController.class).searchParties(searchTerm,
            0, pageSize)).withRel(IanaLinkRelations.FIRST));

        response.add(linkTo(methodOn(PartyController.class).searchParties(searchTerm,
            found_parties.getTotalPages() - 1, pageSize)).withRel(IanaLinkRelations.LAST));

        return ResponseEntity.ok(response);
    }

    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createParty(
        @RequestBody Party.@Valid CreationRequest request
    ) throws InvalidCredentialsException, ResourceNotFoundException {
        ObjectId partyId = partyService.createParty(request);
        return ResponseEntity
            .created(Utils.getUrl(
                "getParty",
                PartyController.class,
                partyId
            ))
            .build();
    }

    @GetMapping(
        path = "{party}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @JsonView(Party.class)
    @PreAuthorize("true")
    public ResponseEntity<Party> getParty(
        @PathVariable("party") @ValidObjectId ObjectId partyId
    ) throws ResourceNotFoundException {
        return ResponseEntity.ok(partyService.getParty(partyId));
    }

    @DeleteMapping("{party}")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('parties', #partyId, #principal)")
    public ResponseEntity<Void> deleteParty(
        @PathVariable("party")
        @ValidObjectId
        ObjectId partyId
    ) throws ResourceNotFoundException {
        partyService.deleteParty(partyId);
        return ResponseEntity.noContent().build();
    }

    // ==== EDITAR PARTY =======================================================

    @PutMapping("{party}/dm")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('parties', #partyId, #principal)")
    public ResponseEntity<Void> changeDm(
        @PathVariable("party")
        @ValidObjectId
        ObjectId partyId,
        @RequestBody
        Party.@Valid DmChangeRequest dm
    ) throws ResourceNotFoundException {
        partyService.changeDm(partyId, dm);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{party}/members")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('parties', #partyId, #principal)")
    public ResponseEntity<Void> addMembers(
        @PathVariable("party")
        @ValidObjectId
        ObjectId partyId,
        @RequestBody
        @Size(min=1, max=20)
        Set<@ValidObjectId ObjectId> newMembers
    ) throws ResourceNotFoundException, LimitException {
        partyService.addMembers(partyId, newMembers);
        return ResponseEntity
            .created(Utils.getUrl(
                "getParty",
                PartyController.class,
                partyId
            ))
            .build();
    }

    @DeleteMapping("{party}/members/{memberid}")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('parties', #partyId, #principal)")
    public ResponseEntity<Void> deleteMember(
        @PathVariable("party")
        @ValidObjectId
        ObjectId partyId,
        @PathVariable("memberid")
        @ValidObjectId
        ObjectId memberId
    ) throws ResourceNotFoundException {
        partyService.deleteMember(partyId, memberId);
        return ResponseEntity.noContent().build();
    }

    // ==== MENSAJES ===========================================================

    @GetMapping("{party}/messages")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('parties', #partyId, #principal) or @auth.isMember(#partyId, #principal)")
    public Slice<Message> getMessages(
        @PathVariable("party")
        @ValidObjectId
        ObjectId partyId,

        @RequestParam(value = "after", required = false, defaultValue = "")
        String after,

        @RequestParam(value = "count", required = false, defaultValue = "10")
        @Min(value = 1, message = "Minimum parties per page is 1")
        @Max(value = 100, message = "Maximum parties per page is 100")
        int count
    ) throws ResourceNotFoundException {
        // TODO: hateoas
        // NOTE: se usa slice porque es un tipo de paginación distinto. Hateoas debería funcionar igual
        return partyService.getMessages(partyId, after, count);
    }

    @PostMapping("{party}/messages")
    @PreAuthorize("hasRole('ADMIN') or @auth.isUserOwner('parties', #partyId, #principal) or @auth.isMember(#partyId, #principal)")
    public ResponseEntity<Void> sendMessages(
        @PathVariable("party")
        @ValidObjectId
        ObjectId partyId,
        @RequestBody
        Message.@Valid CreationRequest message
    ) throws InvalidCredentialsException, ResourceNotFoundException, NoCharacterSelectedException {
        partyService.sendMessage(partyId, message);
        return ResponseEntity
            .created(Utils.getUrl(
                "getMessages",
                PartyController.class,
                partyId, "", 0, 1
            ))
            .build();
    }
}
