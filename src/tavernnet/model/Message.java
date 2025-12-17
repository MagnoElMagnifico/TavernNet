package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    // ==== DTOs ===============================================================

    public record CreationRequest (
        @Nullable String text,
        @Nullable String dice
        // TODO: image
    ) {}

    // ==== ATRIBUTOS ==========================================================

    @Id
    @Valid
    @JsonIgnore
    private final ObjectId id;

    @Valid
    @JsonIgnore
    private final ObjectId author;

    @Valid
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("author")
    private Character.@Nullable Summary authorDetails;

    @JsonIgnore
    private final ObjectId party;

    @Valid
    private final LocalDateTime creation;

    // ---- CONTENIDO ----
    @Nullable
    private final String text;

    // TODO: image

    private final Dice.@Nullable Roll dice;

    // ==== CONSTRUCTORES ======================================================

    @PersistenceCreator
    public Message(
        ObjectId id,
        ObjectId author,
        ObjectId party,
        LocalDateTime creation,
        @Nullable String text,
        Dice.@Nullable Roll dice
    ) {
        this.id = id;
        this.author = author;
        this.party = party;
        this.creation = creation;
        this.text = text;
        this.dice = dice;
    }

    public static Message fromRequest(CreationRequest r, ObjectId author, ObjectId party) {
        return new Message(
            null,
            author,
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

    public ObjectId getAuthor() {
        return author;
    }

    public Character.@Nullable Summary getAuthorDetails() {
        return authorDetails;
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

    public Dice.@Nullable Roll getDice() {
        return dice;
    }

    // ==== OTROS MÉTODOS ======================================================

    public void setAuthorDetails(Character.@Nullable Summary authorDetails) {
        this.authorDetails = authorDetails;
    }

    public void setAuthorDeleted() {
        this.authorDetails = Character.Summary.deleted();
    }

    @JsonIgnore
    @Override
    public String getOwnerId() {
        return author.toHexString();
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
