package tavernnet.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;
import java.util.Objects;

@NullMarked
public class Message implements Ownable {

    // ==== TIPOS DE DATOS ASOCIADOS ===========================================

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = CharacterAuthor.class, name = "CHARACTER"),
        @JsonSubTypes.Type(value = DmAuthor.class, name = "DM")
    })
    public sealed interface Author permits CharacterAuthor, DmAuthor {}

    public record CharacterAuthor (
        @JsonIgnore
        ObjectId characterId,

        @Valid
        @Transient
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("character")
        Character.@Nullable Summary summary
    ) implements Author {}

    public record DmAuthor (
        String name,

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String race,

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer level
    ) implements Author {
        public static DmAuthor asDm() {
            return new DmAuthor("[DM]", null, null);
        }
    }

    // ==== DTOs ===============================================================

    public record CreationRequest (
        @Nullable @Valid DmAuthor author,
        @Nullable String text,
        @Nullable String dice
        // TODO: image
    ) {}

    // ==== ATRIBUTOS ==========================================================

    @Id
    @Valid
    @JsonIgnore
    private final ObjectId id;

    @JsonIgnore
    private final ObjectId party;

    @Valid
    private Author author;

    @Valid
    private final LocalDateTime creation;

    // ---- CONTENIDO ----
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String text;

    // TODO: imagen como mensaje

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Dice.@Nullable Roll roll;

    // ==== CONSTRUCTORES ======================================================

    @PersistenceCreator
    public Message(
        ObjectId id,
        Author author,
        ObjectId party,
        LocalDateTime creation,
        @Nullable String text,
        Dice.@Nullable Roll roll
    ) {
        this.id = id;
        this.author = author;
        this.party = party;
        this.creation = creation;
        this.text = text;
        this.roll = roll;
    }

    public static Message fromRequest(CreationRequest r, ObjectId authorId, ObjectId party) {
        return new Message(
            null,
            Objects.requireNonNullElseGet(r.author, () -> new CharacterAuthor(authorId, null)),
            party,
            LocalDateTime.now(),
            r.text(),
            r.dice() == null? null : Dice.of(r.dice()).roll()
        );
    }

    // ==== GETTERS ============================================================

    public ObjectId getId() {
        return id;
    }

    public Author getAuthor() {
        return author;
    }

    public ObjectId getParty() {
        return party;
    }

    public LocalDateTime getCreation() {
        return creation;
    }

    public @Nullable String getText() {
        return text;
    }

    public Dice.@Nullable Roll getRoll() {
        return roll;
    }

    // ==== OTROS MÉTODOS ======================================================

    public void setAuthorDetails(Character.@Nullable Summary authorDetails) {
        switch (author) {
            case CharacterAuthor character -> this.author = new CharacterAuthor(
                character.characterId,
                authorDetails
            );
            case DmAuthor dm -> { /* no hacer nada */ }
        }
    }

    public void setAuthorDeleted() {
        switch (author) {
            case CharacterAuthor character -> this.author = new CharacterAuthor(
                character.characterId,
                Character.Summary.deleted()
            );
            case DmAuthor dm -> { /* no hacer nada */ }
        }
    }

    @JsonIgnore
    @Override
    public @Nullable String getOwnerId() {
        return switch (author) {
            case CharacterAuthor character -> character.characterId.toHexString();
            case DmAuthor dm -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Message message)) return false;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
