package me.mykindos.betterpvp.core.combat.stats.model;

import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an attachment to a {@link CombatData} object.
 */
public interface ICombatDataAttachment {

    void prepareUpdates(@NotNull CombatData data, @NotNull List<Kill> kills, @NotNull Database database, String databasePrefix);

}
