package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public class Dice {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)d(\\d+)([+-]\\d+)?");

    // ==== TIPOS DE DATOS ASOCIADOS ===========================================

    public enum Type {
        D4(4), D6(6), D8(8), D12(12), D20(20);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public static Type fromInt(int value) {
            for (Type t : values()) {
                if (t.value == value) return t;
            }
            throw new IllegalArgumentException("Invalid dice type: " + value);
        }

        public int toInt() {
            return value;
        }
    }

    // ==== DTOs ===============================================================

    public record Roll (
        @NotBlank
        String dice,
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Collection<Integer> rolls,
        int result
    ) {}

    // ==== ATRIBUTOS ==========================================================

    @Min(value = 1)
    @Max(value = 30)
    private final int number;

    private final Type type;

    @Min(value = -20)
    @Max(value = 20)
    private final int modifier;

    @JsonIgnore
    @Transient
    private final Random random;

    // ==== CONSTRUCTOR ========================================================

    public Dice(int number, Type type, int modifier) {
        this.number = number;
        this.type = type;
        this.modifier = modifier;
        this.random = new Random();
    }

    public static Dice of(int number, int diceType) {
        return new Dice(number, Type.fromInt(diceType), 0);
    }

    public static Dice of(int number, int diceType, int modifier) {
        return new Dice(number, Type.fromInt(diceType), modifier);
    }

    public static Dice of(String value) {
        Matcher m = PATTERN.matcher(value);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid dice string: " + value);
        }

        int number = Integer.parseInt(m.group(1));
        Type type = Type.fromInt(Integer.parseInt(m.group(2)));
        int modifier = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        return new Dice(number, type, modifier);
    }

    // ==== GETTERS ============================================================

    public int getNumber() {
        return number;
    }

    public Type getType() {
        return type;
    }

    public int getModifier() {
        return modifier;
    }

    // ==== OTROS MÉTODOS ======================================================

    public Roll roll() {
        if (number == 1) {
            return new Roll(
                toString(),
                null,
                random.nextInt(type.toInt()) + 1 + modifier
            );
        }

        Collection<Integer> rolls = new ArrayList<>(number);
        int total = 0;
        for (int i = 0; i < number; i++) {
            int rolled = random.nextInt(type.toInt()) + 1;
            total += rolled;
            rolls.add(rolled);
        }
        total += modifier;

        return new Roll(toString(), rolls, total);
    }

    @Override
    public String toString() {
        return number + "d" + type.toInt() + (modifier == 0? "" : "+" + modifier);
    }
}
