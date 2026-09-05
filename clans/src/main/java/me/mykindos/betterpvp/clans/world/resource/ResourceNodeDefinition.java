package me.mykindos.betterpvp.clans.world.resource;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed contents of one {@code scenes/props/<node>.yml} file: the archetype, how it binds to Mapper regions, the
 * profession gate, the loot table, and respawn timing. The archetype-specific block (e.g. {@code tree:}, {@code ore:},
 * {@code fishing:}) is exposed verbatim via {@link #getArchetypeSection()} so each archetype reads its own knobs.
 * <p>
 * {@code fixedSpeed} pins every player's mining speed inside the node to one value, in the block-break framework's
 * scaled units, so a starter pickaxe and a netherite one break the same block in the same time. Omit it (or leave it at
 * zero) and the node mines at whatever the held tool resolves to.
 * <p>
 * {@code respawn} is either a delay in seconds ({@link #getRespawnSeconds()}) or the sentinel {@code none}/{@code
 * never} (case-insensitive), which marks the node {@link #isOneShot()} — harvested once and never restored. Each
 * archetype is responsible for early-outing its own respawn logic when {@link #isOneShot()} is set.
 * <p>
 * Region tags can override {@code level} and {@code displayName} per placement (see {@link RegionTags}).
 */
@Getter
@CustomLog
public final class ResourceNodeDefinition {

    private final String id;
    private final String archetype;
    private final @Nullable String matchName;
    private final @Nullable String profession;
    private final int level;
    private final String displayName;
    private final @Nullable String lootTable;
    private final double respawnSeconds;
    private final boolean oneShot;
    private final int fixedSpeed;
    private final ConfigurationSection root;

    private ResourceNodeDefinition(String id, String archetype, @Nullable String matchName,
                                   @Nullable String profession, int level, String displayName,
                                   @Nullable String lootTable, double respawnSeconds, boolean oneShot,
                                   int fixedSpeed, ConfigurationSection root) {
        this.id = id;
        this.archetype = archetype;
        this.matchName = matchName;
        this.profession = profession;
        this.level = level;
        this.displayName = displayName;
        this.lootTable = lootTable;
        this.respawnSeconds = respawnSeconds;
        this.oneShot = oneShot;
        this.fixedSpeed = fixedSpeed;
        this.root = root;
    }

    /**
     * @return whether this node pins mining speed for everyone inside it
     */
    public boolean hasFixedSpeed() {
        return fixedSpeed > 0;
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
        final RespawnValue respawn = parseRespawn(id, config);
        return new ResourceNodeDefinition(
                id,
                archetype.toLowerCase(),
                matchName,
                config.getString("profession"),
                config.getInt("level", 0),
                displayName,
                config.getString("lootTable"),
                respawn.getSeconds(),
                respawn.isOneShot(),
                Math.max(0, config.getInt("fixedSpeed", 0)),
                config);
    }

    /**
     * Parses {@code respawn} as either a delay in seconds or the sentinel {@code none}/{@code never}
     * (case-insensitive) for a node that never restores. A missing or unparseable value falls back to the 60-second
     * default rather than throwing.
     */
    private static @NotNull RespawnValue parseRespawn(@NotNull String id, @NotNull ConfigurationSection config) {
        final Object raw = config.get("respawn");
        if (raw == null) {
            return new RespawnValue(60.0, false);
        }
        if (raw instanceof Number number) {
            return new RespawnValue(number.doubleValue(), false);
        }
        if (raw instanceof String text) {
            if (text.equalsIgnoreCase("none") || text.equalsIgnoreCase("never")) {
                return new RespawnValue(60.0, true);
            }
            try {
                return new RespawnValue(Double.parseDouble(text.trim()), false);
            } catch (NumberFormatException invalid) {
                log.warn("Node '{}' has an unparseable 'respawn' value '{}' - defaulting to 60 seconds", id, text).submit();
                return new RespawnValue(60.0, false);
            }
        }
        log.warn("Node '{}' has an unparseable 'respawn' value '{}' - defaulting to 60 seconds", id, raw).submit();
        return new RespawnValue(60.0, false);
    }

    /** A parsed {@code respawn} value: the delay to use, and whether it was the one-shot sentinel. */
    @Value
    private static class RespawnValue {
        double seconds;
        boolean oneShot;
    }
}
