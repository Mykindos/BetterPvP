package me.mykindos.betterpvp.core.loot.session;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.loot.LootProgress;
import me.mykindos.betterpvp.core.loot.LootTable;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a loot session.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class LootSession {

    @NotNull final List<LootTable> lootTables;
    @NotNull private final Audience audience;
    @NotNull private final LootProgress progress = new LootProgress();
    @NotNull private final Instant start = Instant.now();
    @Setter(AccessLevel.PRIVATE)
    private Instant end;

    public static LootSession newSession(@NotNull List<LootTable> lootTable, @NotNull Audience audience) {
        final LootSession session = new LootSession(new ArrayList<>(lootTable), audience);
        final LootSessionController controller = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(LootSessionController.class);
        audience.filterAudience(Player.class::isInstance)
                .forEachAudience(single -> controller.pushScope((Player) single, session));
        return session;
    }

    public static LootSession newSession(@NotNull LootTable lootTable, @NotNull Audience audience) {
        return newSession(List.of(lootTable), audience);
    }

    public void end() {
        this.end = Instant.now();
        final LootSessionController controller = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(LootSessionController.class);
        audience.filterAudience(Player.class::isInstance)
                .forEachAudience(single -> controller.removeScope((Player) single, this));
    }

    public List<LootTable> getLootTables() {
        return Collections.unmodifiableList(lootTables);
    }

}
