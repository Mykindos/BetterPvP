package me.mykindos.betterpvp.core.loot;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import me.mykindos.betterpvp.core.loot.session.LootSessions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Stores a set of loot tables for convenience
 */
@Value
@Builder
public class LootController {

    @Singular
    @NotNull Map<@NotNull String, @NotNull LootTable> tables;
    @Singular
    @NotNull Map<@NotNull LootTable, @NotNull LootSessions> sessions;

    public LootTable table(String name) {
        return this.tables.get(name);
    }

    public LootSessions sessions(LootTable table) {
        return this.sessions.get(table);
    }

    public static class LootControllerBuilder {

        public LootControllerBuilder createSessions(boolean pooled) {
            for (LootTable table : tables$value) {
                session(table, pooled ? LootSessions.pooled() : LootSessions.playerBound());
            }
            return this;
        }

    }
}
