package tavernnet.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record DiceSpec(
    @Min(value = 1)
    @Max(value = 30)
    int number,
    DiceType type
) {
    public enum DiceType {
        D4, D6, D8, D12, D20;

        public static DiceType fromInt(int dice) {
            return switch (dice) {
                case 4 -> D4;
                case 6 -> D6;
                case 12 -> D12;
                case 20 -> D20;
                default -> throw new IllegalArgumentException("Expected dice number of 4, 6, 8, 12 or 20; got %d".formatted(dice));
            };
        }

        public int toInt() {
            return switch (this) {
                case D4 -> 4;
                case D6 -> 6;
                case D8 -> 8;
                case D12 -> 12;
                case D20 -> 20;
            };
        }
    }

    public static DiceSpec of(int number, int diceType) {
        return new DiceSpec(number, DiceType.fromInt(diceType));
    }

    @Override
    public String toString() {
        return "%dd%d".formatted(number, type.toInt());
    }
}
