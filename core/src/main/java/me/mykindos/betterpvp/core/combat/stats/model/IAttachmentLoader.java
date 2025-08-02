package me.mykindos.betterpvp.core.combat.stats.model;

import java.util.UUID;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

/**
 * Loads attachments for {@link CombatData} objects.
 */
@FunctionalInterface
public interface IAttachmentLoader<T extends ICombatDataAttachment<? extends CombatData, ? extends Kill>> {

    /**
     * Loads an attachment for the given {@link CombatData} object.
     *
     * @param player   The UUID of the player
     * @param data     The combat data
     * @param database The database
     * @return The attachment
     */
    @NotNull T loadAttachment(@NotNull UUID player, @NotNull CombatData data, @NotNull Database database);

}
