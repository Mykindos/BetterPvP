package me.mykindos.betterpvp.core.world.blocks;

/**
 * Defines how a {@link RestoreBlock} behaves when a player attempts to break it.
 *
 * @param breakable  Whether the block can be broken by players before it expires.
 * @param allowDrops Whether breaking the block produces item drops.
 */
public record DestroyStrategy(boolean breakable, boolean allowDrops) {

    /** Block cannot be broken — all break attempts are cancelled (default behaviour). */
    public static final DestroyStrategy INDESTRUCTIBLE = new DestroyStrategy(false, false);

    /** Block can be broken, but produces no drops. */
    public static final DestroyStrategy BREAKABLE = new DestroyStrategy(true, false);
}
