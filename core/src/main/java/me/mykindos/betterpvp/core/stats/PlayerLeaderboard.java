package me.mykindos.betterpvp.core.stats;

import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class PlayerLeaderboard<T> extends Leaderboard<UUID, T> {

    protected PlayerLeaderboard(BPvPPlugin plugin) {
        super(plugin);
    }

    @Override
    protected LeaderboardEntry<UUID, T> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options, @NotNull Database database) throws UnsupportedOperationException {
        return LeaderboardEntry.of(player, fetch(options, database, player));
    }

    @Override
    protected Description describe(SearchOptions searchOptions, LeaderboardEntry<UUID, T> value) {
        final Map<String, Component> map = describe(searchOptions, value.getValue());
        final Map<String, Component> result = new LinkedHashMap<>();
        final OfflinePlayer player = Bukkit.getOfflinePlayer(value.getKey());
        result.put("Player", Component.text(Objects.requireNonNullElse(player.getName(), "Unknown")));
        result.putAll(map);

        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setPlayerProfile(player.getPlayerProfile());
        itemStack.setItemMeta(meta);

        return Description.builder()
                .icon(itemStack)
                .properties(result)
                .build();
    }

    protected Map<String, Component> describe(SearchOptions searchOptions, T value) {
        if (value instanceof Number number) {
            return Map.of(getName(), Component.text(NumberFormat.getInstance().format(number)));
        } else {
            return Map.of(getName(), Component.text(value.toString()));
        }
    }
}
