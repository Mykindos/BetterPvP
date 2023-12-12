package me.mykindos.betterpvp.core.combat.stats.model;

import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Loads attachments for {@link CombatData} objects.
 */
@FunctionalInterface
public interface IAttachmentLoader {

    /**
     * Loads an attachment for the given {@link CombatData} object.
     *
     * @param player   The UUID of the player
     * @param data     The combat data
     * @param database The database
     * @return The attachment
     */
    @NotNull ICombatDataAttachment loadAttachment(@NotNull UUID player, @NotNull CombatData data, @NotNull Database database);

}
