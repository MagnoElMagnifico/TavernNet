package tavernnet.controller;

import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.*;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.Party;
import tavernnet.service.CharacterService;
import tavernnet.model.Character;
import tavernnet.utils.patch.JsonPatchOperation;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("users")
@NullMarked
public class CharacterController {

    private final CharacterService characterService;

    @Autowired
    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    // Servicio para obtener todos los personajes de un usuario
    @GetMapping("{userid}/characters")
    @PreAuthorize("true")
    public Collection<Character> getCharacters(
        @PathVariable("userid") @NotBlank String id
    ) throws ResourceNotFoundException {
        return characterService.getCharactersByUser(id);
    }

    /**
     * <code>POST /users/{userid}/characters/</code>
     * @param newCharacter Nueva publicación.
     * @return <code>201 Created</code> en éxito.
     */
    @PostMapping("{userid}/characters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createCharacter(
        @PathVariable("userid") @NotBlank String userId,
        @RequestBody @Valid Character.CreationRequest newCharacter
    ) throws DuplicatedResourceException, ResourceNotFoundException, LimitException {
            String newId = characterService.createCharacter(newCharacter, userId);

            var url = MvcUriComponentsBuilder.fromMethodName(
                    CharacterController.class,
                    "getCharacter",
                    userId,
                    newId)
                .build()
                .toUri();

            return ResponseEntity.created(url).build();
    }

    /**
     * <code>GET /users/{userid}/characters/{characterName}</code>
     * @param userId Identificador del usuario.
     * @param characterName Nombre del character.
     * @return Personaje solicitado
     */
    @GetMapping("{userid}/characters/{characterName}")
    @PreAuthorize("true")
    public Character getCharacter(
        @PathVariable("userid") @NotBlank String userId,
        @PathVariable("characterName") @NotBlank String characterName
    ) throws ResourceNotFoundException {
        return characterService.getCharacter(userId, characterName);
    }

    @PatchMapping("{userid}/characters/{characterName}")
    @PreAuthorize("hasRole('ADMIN') or @auth.isCharacterOwnerByName(#username, #characterName, principal)")
    public Character updateCharacter(
        @PathVariable("userid") @NotBlank String username,
        @PathVariable("characterName") @NotBlank String characterName,
        @RequestBody @Valid List<@Valid JsonPatchOperation> changes
    ) throws ResourceNotFoundException {
        return characterService.updateCharacter(username, characterName, changes);
    }

    /**
     * @param username Nombre del usuario al que pertenece el personaje
     * @param characterName Nombre del personaje a borrar
     * @throws ResourceNotFoundException Si el ID no existe
     */
    @DeleteMapping("{userid}/characters/{characterName}")
    @PreAuthorize("hasRole('ADMIN') or @auth.isCharacterOwnerByName(#username, #characterName, principal)")
    public ResponseEntity<Void> deleteCharacter(
        @PathVariable("userid") @NotBlank String username,
        @PathVariable("characterName") @NotBlank String characterName
    ) throws ResourceNotFoundException {
        characterService.deleteCharacter(username, characterName);
        return ResponseEntity.noContent().build();
    }
}
