package me.mykindos.betterpvp.clans.clans.leveling;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;

public interface ClanPerk {

    /**
     * A unique, stable string identifier for this perk instance.
     * Used as the key in {@link ClanPerkManager}. Must be unique across all registered perks.
     */
    String getPerkId();

    /**
     * Get the name of the perk.
     */
    String getName();

    /**
     * Get the minimum level required to unlock the perk.
     */
    int getMinimumLevel();

    /**
     * Get the description of the perk.
     */
    Component[] getDescription();

    /**
     * Get the icon of the perk shown in menus.
     */
    ItemView getIcon();

    /**
     * The category this perk belongs to, used for UI grouping and filtering.
     */
    ClanPerkCategory getCategory();

    /**
     * Check if a clan at the given level has this perk unlocked.
     * Callers should compute the level via {@link ClanExperience}.
     */
    default boolean hasPerk(long clanLevel) {
        return clanLevel >= getMinimumLevel();
    }

    /**
     * Called once when a clan first reaches this perk's level threshold.
     * Override for perks that need one-time side effects on unlock (e.g. unlocking a warp slot).
     * Default: no-op.
     */
    default void onUnlock(me.mykindos.betterpvp.clans.clans.Clan clan) {}

    /**
     * Called once when a clan drops below this perk's level threshold.
     * Default: no-op.
     */
    default void onLock(me.mykindos.betterpvp.clans.clans.Clan clan) {}

}
