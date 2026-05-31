package me.mykindos.betterpvp.core.world.zone;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * A region of space players can occupy, resolved by the {@link ZoneManager}. Replaces the old per-module zone enums
 * with a buildable instance: identity is an Adventure {@link Key}, the spatial extent is a pluggable
 * {@link ZoneBounds}, and per-instance behaviour hangs off {@link #getRules() rules} and {@link #getTags() tags}.
 * <p>
 * Build dynamic zones with {@link #builder()}; reusable, world-independent zones live as static members on
 * {@link Zones} (mirroring {@code SocketableGroups}).
 * <p>
 * Equality and hashing are by {@link #getKey() key} alone, so a zone can be compared, indexed, and looked up by its
 * identity regardless of its current bounds.
 */
@Getter
public final class Zone {

    /** Stable identity. Equality is by this key. */
    private final Key key;
    /** Human-facing name. */
    private final Component displayName;
    /** Spatial extent and containment strategy. */
    private final ZoneBounds bounds;
    /** Resolution priority; on overlap the highest priority wins. */
    private final int priority;
    /** Free-form markers consumers and rules can branch on (see {@link Zones}). */
    private final Set<String> tags;
    /** Rules attached directly to this zone (composition half of the rule system). */
    private final ZoneRuleContainer rules;

    @Builder
    private Zone(@NotNull Key key,
                @Nullable Component displayName,
                @NotNull ZoneBounds bounds,
                int priority,
                @Singular Set<String> tags,
                @Nullable ZoneRuleContainer rules) {
        this.key = Objects.requireNonNull(key, "key");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.displayName = displayName != null ? displayName : Component.text(UtilFormat.cleanString(key.value()));
        this.priority = priority;
        this.tags = Set.copyOf(tags);
        this.rules = rules != null ? rules : new ZoneRuleContainer();
    }

    /**
     * @param location the location to test
     * @return whether this zone contains the location
     */
    public boolean contains(@NotNull Location location) {
        return bounds.contains(location);
    }

    /**
     * @param other another zone's key
     * @return whether this zone has that identity
     */
    public boolean is(@NotNull Key other) {
        return key.equals(other);
    }

    public boolean hasTag(@NotNull String tag) {
        return tags.contains(tag);
    }

    /**
     * @return the world this zone lives in, or {@code null} if unbound
     */
    public @Nullable World getWorld() {
        return bounds.getWorld();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Zone zone && key.equals(zone.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "Zone(" + key.asString() + ")";
    }
}
