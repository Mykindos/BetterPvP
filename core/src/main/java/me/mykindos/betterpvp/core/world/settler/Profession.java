package me.mykindos.betterpvp.core.world.settler;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a settler does. A settler's profession is its role, so a profession decides where it works.
 * Specialties are the kinds a profession comes in, one of which every settler of it rolls, such as a Builder's trade.
 */
@Value
public class Profession {

    @NotNull String id;
    /** Translation key of its name. Each specialty's name is under {@code <key>.specialty.<specialty>}. */
    @NotNull String key;
    @NotNull WorkplaceKind workplaceKind;
    /** The workplace a {@link WorkplaceKind#WORKPLACE} profession works at, null for construction. */
    @Nullable String workplace;
    @NotNull List<String> specialties;

    public static @NotNull Profession construction(@NotNull String id, @NotNull String key,
                                                   @NotNull List<String> specialties) {
        return new Profession(id, key, WorkplaceKind.CONSTRUCTION, null, List.copyOf(specialties));
    }

    public static @NotNull Profession workplace(@NotNull String id, @NotNull String key, @NotNull String workplace) {
        return new Profession(id, key, WorkplaceKind.WORKPLACE, workplace, List.of());
    }

    public @NotNull String specialtyKey(@NotNull String specialty) {
        return key + ".specialty." + specialty;
    }
}
