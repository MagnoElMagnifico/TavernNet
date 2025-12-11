package tavernnet.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tavernnet.utils.patch.JsonPatchOperation;
import tavernnet.utils.patch.exceptions.JsonPatchFailedException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Document(collection = "characters")
@NullMarked
public record Character(
    // TODO: esto se serializa como objeto y no como string
    @Id ObjectId id,
    @NotBlank @Size(max = 50, message = "Character name too long") String name,
    @NotBlank String user,
    @Nullable @Size(max = 1000, message = "Biography too long") String biography,
    @NotBlank @Size(max = 50, message = "Race field too long") String race,
    Collection<@NotBlank String> languages,
    LocalDateTime creation,
    @Valid Alignment alignment,
    @Valid Stats stats,
    @Valid Stats modifiers,
    @Valid CombatStats combat,
    @Valid PassiveStats passive,
    Collection<@Valid Action> actions
) implements Ownable {

    public static Character defaultCharacter(
        ObjectId id,
        String name,
        String username,
        String biography,
        String race,
        Collection<String> languages
    ) {
        return new Character(
            id,
            name,
            username,
            biography,
            race,
            languages,
            LocalDateTime.now(),
            Alignment.TRUE_NEUTRAL,
            Stats.defaultGeneralStats(),
            Stats.defaultModifiers(),
            CombatStats.defaultStats(),
            PassiveStats.defaultStats(),
            Action.defaultActions()
        );
    }

    public Character(CreationRequest request, String username) {
        this(
            null, // desconocido hasta insertar en la DB
            request.name,
            username,
            request.biography,
            request.race,
            request.languages,
            LocalDateTime.now(),
            request.alignment,
            request.general == null? Stats.defaultGeneralStats() : request.general,
            request.general == null? Stats.defaultModifiers() : Stats.asModifiers(request.general),
            request.combat == null? CombatStats.defaultStats() : request.combat,
            request.passive == null? PassiveStats.defaultStats() : request.passive,
            request.actions
        );
    }

    @Override
    public String getOwnerId() {
        return user;
    }

    public static void validatePatch(JsonPatchOperation op) {
        if (op == null || op.path() == null) {
            return;
        }

        switch (op.path().toString()) {
            case "/id", "/creation", "/user" -> throw new JsonPatchFailedException(
                "Changing ID, user or creation date is forbidden"
            );
        }
    }

    // ==== TIPOS DE DATOS ASOCIADOS ===========================================

    public enum Alignment {
        LAWFUL_GOOD,
        LAWFUL_NEUTRAL,
        LAWFUL_EVIL,
        NEUTRAL_GOOD,
        TRUE_NEUTRAL,
        NEUTRAL_EVIL,
        CHAOTIC_GOOD,
        CHAOTIC_NEUTRAL,
        CHAOTIC_EVIL,
    }

    public record Stats (
        @Min(value = -10) @Max(value = 30) int constitution,
        @Min(value = -10) @Max(value = 30) int dexterity,
        @Min(value = -10) @Max(value = 30) int strength,
        @Min(value = -10) @Max(value = 30) int wisdom,
        @Min(value = -10) @Max(value = 30) int charisma
    ) {

        // Valores por defecto
        public static Stats defaultGeneralStats() {
            return new Stats(10, 10, 10, 10, 10);
        }

        // Calcular un modificador a partir de un valor según las reglas estándar
        public static int modifierOf(int value) {
            //                        3 4/5 6/7 8/9 10/11 12/13 14/15 16/17 18/19  20
            final int[] modifiers = {-4, -3, -2, -1,   +0,   +1,   +2,   +3,   +4, +5};
            int index = (int) Math.floor(((float) (value - 2) / 2.0f));
            return modifiers[Math.clamp(index, 0, modifiers.length - 1)];
        }

        // Obtiene los modificadores de las estadísticas según las reglas estándar
        public static Stats asModifiers(Stats general) {
            return new Stats(
                modifierOf(general.constitution),
                modifierOf(general.dexterity),
                modifierOf(general.strength),
                modifierOf(general.wisdom),
                modifierOf(general.charisma)
            );
        }

        public static Stats defaultModifiers() {
            return new Stats(0, 0, 0, 0, 0);
        }
    }

    public record CombatStats (
        @Min(value = 10) @Max(value = 30) int ac,
        @Min(value = -20) @Max(value = 1000) int hp,
        @Min(value = 0) @Max(value = 100) int speed,
        @Min(value = -10) @Max(value = 15) int initiative
    ) {
        public static CombatStats defaultStats() {
            return new CombatStats(12, 10, 30, 0);
        }
    }

    public record PassiveStats (
        @Min(value = -10) @Max(value = 15) int perception
    ) {
        public static PassiveStats defaultStats() {
            return new PassiveStats(12);
        }
    }

    public record Action (
        @NotBlank @Size(max = 50, message = "Action name too long") String name,
        @NotBlank @Size(max = 1000, message = "Action description too long") String description,
        @Min(value = 0) @Max(value = 1000) int range,
        @Min(value = -20) @Max(value = 20) int toHit,
        @Valid DiceSpec damageDice,
        @NotBlank String damageType,
        @Valid Type type
    ) {
        public enum Type {
            MELEE, RANGED, MAGIC
        }

        public static Collection<Action> defaultActions() {
            return List.of(
                new Action("Unarmed strike", "Regular attack with no weapons", 5, 0, DiceSpec.of(1, 6), "bludgeoning", Type.MELEE)
            );
        }
    }

    // ==== DTOs ===============================================================

    public record CreationRequest (
        @NotBlank @Size(max = 50, message = "Character name too long") String name,
        @Nullable @Size(max = 1000, message = "Biography too long") String biography,
        @NotBlank @Size(max = 50, message = "Race field too long") String race,
        @NotNull Collection<@NotBlank String> languages,
        @Valid Alignment alignment,
        @Valid @Nullable Stats general,
        @Valid @Nullable CombatStats combat,
        @Valid @Nullable PassiveStats passive,
        @NotNull Collection<@Valid Action> actions
    ) {}
}
