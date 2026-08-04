package me.mykindos.betterpvp.clans.world.ship;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one mooring across the whole server, as {@code <world>/<berth>}.
 * <p>
 * A berth id is only unique within its world, and instanced worlds are named {@code islands/<template>/<instance>} —
 * so the key contains slashes of its own and must be split from the <em>right</em>. Splitting from the left would read
 * a berth in {@code islands/mining/a1b2c3d4} as living in a world called {@code islands}.
 */
@UtilityClass
public class BerthKey {

    private static final char SEPARATOR = '/';

    public static @NotNull String of(@NotNull String worldName, @NotNull String berthId) {
        return worldName + SEPARATOR + berthId;
    }

    public static @NotNull String of(@NotNull Berth berth) {
        return of(berth.getWorldName(), berth.getId());
    }

    public static @NotNull String worldOf(@NotNull String key) {
        final int split = key.lastIndexOf(SEPARATOR);
        return split < 0 ? "" : key.substring(0, split);
    }

    public static @NotNull String berthOf(@NotNull String key) {
        final int split = key.lastIndexOf(SEPARATOR);
        return split < 0 ? key : key.substring(split + 1);
    }
}
