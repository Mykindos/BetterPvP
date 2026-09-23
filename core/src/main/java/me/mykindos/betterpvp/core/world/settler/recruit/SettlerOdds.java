package me.mykindos.betterpvp.core.world.settler.recruit;

import lombok.Value;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;

/** How likely each rarity and profession is for a settler turning up somewhere. Weights, not percentages. */
@Value
public class SettlerOdds {

    /** The profession key that means a settler with no profession. */
    public static final String NONE = "none";

    @NotNull Map<SettlerRarity, Double> rarities;
    @NotNull Map<String, Double> professions;

    public @NotNull SettlerRarity rarity(@NotNull Random random) {
        final SettlerRarity rolled = pick(rarities, random);
        return rolled == null ? SettlerRarity.COMMON : rolled;
    }

    /** A profession id, or null for none. */
    public @Nullable String profession(@NotNull Random random) {
        final String rolled = pick(professions, random);
        return rolled == null || NONE.equals(rolled) ? null : rolled;
    }

    /** One key of {@code weights}, each as likely as its weight, or null when no weight is above zero. */
    public static <T> @Nullable T pick(@NotNull Map<T, Double> weights, @NotNull Random random) {
        final double total = weights.values().stream().mapToDouble(weight -> Math.max(0, weight)).sum();
        if (total <= 0) {
            return null;
        }
        double roll = random.nextDouble() * total;
        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            roll -= Math.max(0, entry.getValue());
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return null;
    }
}
