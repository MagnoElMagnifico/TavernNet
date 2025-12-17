package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tavernnet.utils.ValidObjectId;

import java.util.ArrayList;
import java.util.Collection;

@Document(collection = "parties")
@NullMarked
public class Party implements Ownable {

    // ==== DTOs Y TIPOS DE DATOS ASOCIADOS ====================================

    @NullMarked
    public record Summary (
        @ValidObjectId String id,
        @NotBlank String name,
        int members
    ) {}

    @NullMarked
    public record CreationRequest (
        // NOTA: el DM se lee de la autenticacion
        @Nullable String name,
        @Nullable String description,
        @JsonProperty("initial_members")
        @Nullable @Size(max=20) Collection<@NotBlank String> inicialMembers
    ) {}

    public record DmChangeRequest (
        @NotBlank String username
    ) {}

    // ==== ATRIBUTOS ==========================================================

    @Id
    @JsonIgnore
    @ValidObjectId
    private final ObjectId id;

    @Transient
    @ValidObjectId
    private final String idStr;

    @NotBlank
    private final String name;

    @Nullable
    private final String description;

    @NotBlank
    private final String dm;

    @Field("members")
    @JsonIgnore
    @Size(max=20)
    private final Collection<@ValidObjectId ObjectId> membersIds;

    @Transient
    @Nullable
    @Size(max=20)
    private Collection<Character.Summary> memberDetails;

    // ==== CONSTRUCTORES ======================================================

    public Party(
        @ValidObjectId ObjectId id,
        @NotBlank String name,
        @Nullable String description,
        @NotBlank String dm,
        Collection<@ValidObjectId ObjectId> membersIds
    ) {
        this.id = id;
        this.idStr = id == null? null : id.toHexString();
        this.name = name;
        this.description = description;
        this.dm = dm;
        this.membersIds = membersIds;
    }

    public static Party fromRequest(@Valid CreationRequest r, @NotBlank String dm) {
        return new Party(
            null,
            r.name == null ? "%s's party".formatted(dm) : r.name,
            r.description,
            dm,
            r.inicialMembers == null
                ? new ArrayList<>()
                : r.inicialMembers.stream().map(ObjectId::new).toList()
        );
    }

    // ==== GETTERS ============================================================

    public String getName() {
        return name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public ObjectId getId() {
        return id;
    }

    public String getDm() {
        return dm;
    }

    public Collection<ObjectId> getMembersIds() {
        return membersIds;
    }

    public @Nullable Collection<Character.Summary> getMemberDetails() {
        return memberDetails;
    }

    // ==== OTROS MÉTODOS ======================================================

    public void setMemberDetails(@Nullable Collection<Character.Summary> members) {
        memberDetails = members;
    }

    @JsonIgnore
    @Override
    public String getOwnerId() {
        return dm;
    }
}
