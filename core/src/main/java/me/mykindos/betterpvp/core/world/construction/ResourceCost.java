package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An amount of each resource, by resource id. Core does not know what the resources are, only how to add, scale and
 * compare amounts of them.
 */
@Value
@Builder
@Jacksonized
public class ResourceCost {

    public static final ResourceCost NONE = ResourceCost.builder().build();

    @Singular
    Map<String, Integer> amounts;

    public static @NotNull ResourceCost of(@NotNull Map<String, Integer> amounts) {
        return ResourceCost.builder().amounts(amounts).build();
    }

    public int get(@NotNull String resource) {
        return amounts.getOrDefault(resource, 0);
    }

    @JsonIgnore
    public boolean isFree() {
        return amounts.values().stream().allMatch(amount -> amount <= 0);
    }

    /** This cost scaled by {@code fraction}, rounded down per resource, for a partial refund. */
    public @NotNull ResourceCost share(double fraction) {
        final double clamped = Math.clamp(fraction, 0.0, 1.0);
        final Map<String, Integer> scaled = new LinkedHashMap<>();
        amounts.forEach((resource, amount) -> scaled.put(resource, (int) Math.floor(amount * clamped)));
        return of(scaled);
    }

    public @NotNull ResourceCost plus(@NotNull ResourceCost other) {
        final Map<String, Integer> sum = new LinkedHashMap<>(amounts);
        other.amounts.forEach((resource, amount) -> sum.merge(resource, amount, Integer::sum));
        return of(sum);
    }
}
