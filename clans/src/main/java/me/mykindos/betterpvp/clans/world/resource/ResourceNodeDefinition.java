package me.mykindos.betterpvp.clans.world.resource;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed contents of one {@code scenes/props/<node>.yml} file: the archetype, how it binds to Mapper regions, the
 * profession gate, the loot table, and respawn timing. The archetype-specific block (e.g. {@code tree:}, {@code ore:},
 * {@code fishing:}) is exposed verbatim via {@link #getArchetypeSection()} so each archetype reads its own knobs.
 * <p>
 * Region tags can override {@code level} and {@code displayName} per placement (see {@link RegionTags}).
 */
@Getter
public final class ResourceNodeDefinition {

    private final String id;
    private final String archetype;
    private final @Nullable String matchName;
    private final @Nullable String profession;
    private final int level;
    private final String displayName;
    private final @Nullable String lootTable;
    private final double respawnSeconds;
    private final ConfigurationSection root;

    private ResourceNodeDefinition(String id, String archetype, @Nullable String matchName,
                                   @Nullable String profession, int level, String displayName,
                                   @Nullable String lootTable, double respawnSeconds, ConfigurationSection root) {
        this.id = id;
        this.archetype = archetype;
        this.matchName = matchName;
        this.profession = profession;
        this.level = level;
        this.displayName = displayName;
        this.lootTable = lootTable;
        this.respawnSeconds = respawnSeconds;
        this.root = root;
    }

    /**
     * @return the archetype-specific configuration sub-section (e.g. {@code tree}), or null if absent
     */
    public @Nullable ConfigurationSection getArchetypeSection() {
        return root.getConfigurationSection(archetype);
    }

    /**
     * Parses a node definition from a loaded config. Returns null (with no exception) if the required {@code archetype}
     * key or a region selector ({@code match.tag} / {@code match.name}) is missing.
     *
     * @param id     the node id (typically the file name without extension)
     * @param config the file's root section
     */
    public static @Nullable ResourceNodeDefinition from(@NotNull String id, @NotNull ConfigurationSection config) {
        final String archetype = config.getString("archetype");
        if (archetype == null || archetype.isBlank()) {
            return null;
        }
        final String matchName = config.getString("match.name");
        if (matchName == null || matchName.isBlank()) {
            return null;
        }
        final String displayName = config.getString("displayName", id);
        return new ResourceNodeDefinition(
                id,
                archetype.toLowerCase(),
                matchName,
                config.getString("profession"),
                config.getInt("level", 0),
                displayName,
                config.getString("lootTable"),
                config.getDouble("respawn", 60.0),
                config);
    }
}
