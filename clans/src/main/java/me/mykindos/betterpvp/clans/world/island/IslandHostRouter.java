package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves which server hosts a given {@link IslandTemplate}, from the {@code islands.hosting} block in
 * {@code islands.yml}. A template with no entry there defaults to the current server, which is why every island runs
 * entirely locally until a hosting entry actually points elsewhere.
 */
@CustomLog
@Singleton
public class IslandHostRouter {

    private final Map<String, String> hosts = new HashMap<>();

    @Inject
    public IslandHostRouter(@NotNull Clans clans) {
        reload(clans);
    }

    /**
     * @return the name of the server that hosts {@code template}
     */
    public @NotNull String hostFor(@NotNull IslandTemplate template) {
        return hosts.getOrDefault(template.getKey(), Core.getCurrentRealm().getServer().getName());
    }

    /**
     * @return whether {@code template} is hosted by the server this code is currently running on
     */
    public boolean isLocal(@NotNull IslandTemplate template) {
        return hostFor(template).equals(Core.getCurrentRealm().getServer().getName());
    }

    /**
     * @return whether this server can currently offer {@code template} to a player at all, whether that means
     * allocating it locally or, once remote allocation exists, on whichever server hosts it remotely
     */
    public boolean isServable(@NotNull IslandTemplate template) {
        return isLocal(template);
    }

    private void reload(@NotNull Clans clans) {
        hosts.clear();

        final ExtendedYamlConfiguration config = clans.getConfig("islands");
        final ConfigurationSection hosting = config.getConfigurationSection("hosting");
        if (hosting == null) {
            return;
        }

        for (String key : hosting.getKeys(false)) {
            final String server = hosting.getString(key);
            if (server == null || server.isBlank()) {
                continue;
            }
            hosts.put(key, server);
        }

        log.info("Loaded {} island hosting override(s)", hosts.size()).submit();
    }

}
