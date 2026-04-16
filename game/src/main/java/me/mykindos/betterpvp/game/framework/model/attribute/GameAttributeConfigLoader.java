package me.mykindos.betterpvp.game.framework.model.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.GameRegistry;
import me.mykindos.betterpvp.game.framework.event.GameChangeEvent;
import me.mykindos.betterpvp.game.guice.GameModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;

/**
 * Loads per-game attribute overrides from {@code plugins/Game/games/<id>.yml}.
 * <p>
 * When a game is selected, this reads the corresponding YAML file and applies any
 * overrides to the registered {@link GameAttribute} instances, updating both the
 * current value and the default value (so "/game attribute … reset" resets to the
 * configured value rather than the code-level default).
 * <p>
 * YAML format mirrors the attribute key structure using nested sections:
 * <pre>
 * game:
 *   required-players: 10
 *   max-players: 24
 *   respawn-timer: 8.0
 * </pre>
 * Only keys listed in the file are overridden; absent keys keep their code defaults.
 */
@Singleton
@CustomLog
@BPvPListener
public class GameAttributeConfigLoader implements Listener {

    private final GamePlugin plugin;
    private final GameAttributeManager attributeManager;
    private final GameRegistry gameRegistry;

    @Inject
    public GameAttributeConfigLoader(GamePlugin plugin, GameAttributeManager attributeManager, GameRegistry gameRegistry) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
        this.gameRegistry = gameRegistry;
    }

    /**
     * Runs at NORMAL priority so that bound attributes are already registered
     * (GameRegistry enters the game scope at LOWEST priority, which triggers
     * GenericGameConfiguration.setAttributes() during child-injector creation).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onGameChange(GameChangeEvent event) {
        if (event.getNewGame() == null) return;

        GameModule module = gameRegistry.getModule(event.getNewGame());
        if (module == null) return;

        String gameId = module.getId().toLowerCase();
        File configFile = new File(plugin.getDataFolder(), "games/" + gameId + ".yml");

        if (!configFile.exists()) {
            saveDefaults(gameId, configFile);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        int applied = 0;
        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key)) continue;

            GameAttribute<?> attribute = attributeManager.getAttribute(key);
            if (attribute == null) {
                log.warn("Unknown attribute '{}' in games/{}.yml — skipping", key, gameId).submit();
                continue;
            }

            String rawValue = config.getString(key);
            if (rawValue == null) continue;

            if (applyOverride(attribute, key, rawValue, gameId)) {
                applied++;
            }
        }

        if (applied > 0) {
            log.info("Applied {} attribute override(s) for game '{}'", applied, gameId).submit();
        }
    }

    /**
     * Parses {@code rawValue} using the attribute's own parser, then sets both the
     * default value and the current value so that resets land on the configured default.
     */
    private <T> boolean applyOverride(GameAttribute<T> attribute, String key, String rawValue, String gameId) {
        T parsed = attribute.parseValue(rawValue);
        if (parsed == null) {
            log.warn("Could not parse value '{}' for attribute '{}' in games/{}.yml", rawValue, key, gameId).submit();
            return false;
        }
        attribute.setDefaultValue(parsed);
        attribute.setValue(parsed);
        return true;
    }

    private <T> String formatValue(GameAttribute<T> attribute) {
        return attribute.formatValue(attribute.getValue());
    }

    /**
     * Creates a starter config file populated with all currently registered bound
     * attributes and their code-level default values, so admins have a ready-made
     * template to edit.
     */
    private void saveDefaults(String gameId, File configFile) {
        configFile.getParentFile().mkdirs();
        YamlConfiguration defaults = new YamlConfiguration();

        for (GameAttribute<?> attribute : attributeManager.getAttributes()) {
            if (!(attribute instanceof BoundAttribute<?>)) continue;
            defaults.set(attribute.getKey(), attribute.getValue());
        }

        if (defaults.getKeys(false).isEmpty()) return;

        try {
            defaults.save(configFile);
            log.info("Created default attribute config for game '{}' at {}", gameId, configFile.getPath()).submit();
        } catch (IOException e) {
            log.error("Failed to save default attribute config for game '{}'", gameId, e).submit();
        }
    }
}
