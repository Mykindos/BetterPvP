package me.mykindos.betterpvp.core.combat.stats.model;

import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an attachment to a {@link CombatData} object.
 */
public interface ICombatDataAttachment<T extends CombatData, K extends Kill> {

    void prepareUpdates(@NotNull T data, @NotNull Database database);

    void onKill(@NotNull T data, K kill);

}
