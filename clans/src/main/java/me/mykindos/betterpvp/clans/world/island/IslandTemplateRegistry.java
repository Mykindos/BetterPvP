package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the discovery island templates declared in {@code islands.yml} — one entry per template id, each pointing at
 * a hand-authored world folder that {@link IslandWorldProvisioner} clones.
 */
@CustomLog
@Singleton
public class IslandTemplateRegistry {

    private final Map<String, IslandTemplate> templates = new LinkedHashMap<>();

    @Inject
    public IslandTemplateRegistry(@NotNull Clans clans) {
        reload(clans);
    }

    public @NotNull Optional<IslandTemplate> get(@NotNull String key) {
        return Optional.ofNullable(templates.get(key));
    }

    public @NotNull Collection<IslandTemplate> all() {
        return templates.values();
    }

    private void reload(@NotNull Clans clans) {
        templates.clear();

        final ExtendedYamlConfiguration config = clans.getConfig("islands");
        final Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            if (key.equals("hosting")) {
                // Not a template - IslandHostRouter reads this section for which server hosts each template.
                continue;
            }

            final ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                log.warn("Invalid island template section: {}", key).submit();
                continue;
            }

            final String displayName = section.getString("display-name", key);
            final String templateFolder = section.getString("template-folder");
            if (templateFolder == null || templateFolder.isBlank()) {
                log.warn("Island template '{}' is missing 'template-folder' - skipping", key).submit();
                continue;
            }

            final Material icon = Material.matchMaterial(section.getString("icon", "GRASS_BLOCK"));
            final VoyageTiming timing = VoyageTiming.of(
                    section.getInt("min-seconds", VoyageTiming.DEFAULT.getMinSeconds()),
                    section.getInt("max-seconds", VoyageTiming.DEFAULT.getMaxSeconds()),
                    section.getDouble("chance", VoyageTiming.DEFAULT.getChancePerRoll()));

            templates.put(key, new IslandTemplate(key, Component.text(displayName), templateFolder,
                    icon != null ? icon : Material.GRASS_BLOCK, timing));
        }

        log.info("Loaded {} discovery island template(s)", templates.size()).submit();
    }

}
