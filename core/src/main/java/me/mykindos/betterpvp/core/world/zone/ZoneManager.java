package me.mykindos.betterpvp.core.world.zone;

import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry and resolver for {@link Zone}s. Owns a built-in chunk-bucket index ({@link IndexedZoneProvider})
 * for spatial zones, accepts external {@link ZoneProvider}s for modules that store ownership their own way (e.g. clans
 * via chunk PDC), tracks each player's current zone for enter/exit events, and exposes the hybrid access hook
 * {@link #queryAccess}.
 * <p>
 * Resolution is near-O(1): the index packs a chunk key and checks the handful of zones bucketed there; ambient
 * predicate zones (kept few) and external providers are also consulted; on overlap the highest {@link Zone#getPriority()}
 * wins, ties broken by encounter order. When nothing matches, the location's world {@link #setDefaultZone default zone}
 * (if any) is returned.
 */
@Singleton
public class ZoneManager {

    private final IndexedZoneProvider index = new IndexedZoneProvider();
    private final List<ZoneProvider> providers = new CopyOnWriteArrayList<>();
    /** Ambient (non-chunk-indexable) zones, evaluated on every resolution. Kept small by design. */
    private final List<Zone> ambient = new CopyOnWriteArrayList<>();
    private final Map<Key, Zone> registered = new ConcurrentHashMap<>();
    private final Map<UUID, Zone> defaultZones = new ConcurrentHashMap<>();
    private final Map<Player, Zone> playerZones = new WeakHashMap<>();

    public ZoneManager() {
        providers.add(index);
    }

    // <editor-fold desc="Registration">

    /**
     * Registers a zone. Chunk-indexable zones go into the spatial index; ambient zones (no covered chunks) are added
     * to the always-checked set.
     *
     * @param zone the zone to register
     * @return the same zone, for chaining
     */
    public Zone register(@NotNull Zone zone) {
        registered.put(zone.getKey(), zone);
        if (zone.getBounds().coveredChunks().isEmpty()) {
            ambient.add(zone);
        } else {
            index.add(zone);
        }
        return zone;
    }

    public void unregister(@NotNull Zone zone) {
        registered.remove(zone.getKey());
        ambient.remove(zone);
        index.remove(zone);
    }

    /**
     * Adds an external zone source consulted during resolution alongside the built-in index.
     *
     * @param provider the provider to add
     */
    public void addProvider(@NotNull ZoneProvider provider) {
        providers.add(provider);
    }

    /**
     * Sets the fallback zone for a world, returned by {@link #getZoneAt} when no other zone matches a location in that
     * world. This is how a "default" area (e.g. the hub common area) is modelled.
     *
     * @param world the world
     * @param zone  the fallback zone
     */
    public void setDefaultZone(@NotNull World world, @NotNull Zone zone) {
        defaultZones.put(world.getUID(), zone);
        registered.put(zone.getKey(), zone);
    }

    /**
     * @return whether any zone, provider, or default is registered. Resolution short-circuits when this is false, so
     * servers that never use zones pay nothing on movement.
     */
    public boolean isActive() {
        return !registered.isEmpty() || providers.size() > 1 || !defaultZones.isEmpty();
    }

    public @NotNull Optional<Zone> getZone(@NotNull Key key) {
        return Optional.ofNullable(registered.get(key));
    }

    // </editor-fold>

    // <editor-fold desc="Resolution">

    /**
     * Resolves the single highest-priority zone at a location, or the world's default zone, or {@code null}.
     * <p>
     * Walks every provider (the chunk index first) and the ambient set, keeping the highest {@link Zone#getPriority()};
     * a strictly greater priority replaces the incumbent, so on equal priority the first encountered wins.
     *
     * @param location the location to resolve
     * @return the resolved zone, or {@code null} if none and no world default applies
     */
    public @Nullable Zone getZoneAt(@NotNull Location location) {
        Zone best = null;
        for (ZoneProvider provider : providers) {
            best = highest(best, provider.zonesAt(location).iterator());
        }
        for (Zone zone : ambient) {
            if (zone.contains(location) && (best == null || zone.getPriority() > best.getPriority())) {
                best = zone;
            }
        }
        if (best != null) {
            return best;
        }
        final World world = location.getWorld();
        return world == null ? null : defaultZones.get(world.getUID());
    }

    /**
     * @param location the location to resolve
     * @return every zone containing the location (across all providers and ambient zones), unordered. Does not include
     * the world default. Use when a caller needs all overlapping zones rather than just the winner.
     */
    public @NotNull List<Zone> getZonesAt(@NotNull Location location) {
        final List<Zone> result = new ArrayList<>();
        for (ZoneProvider provider : providers) {
            provider.zonesAt(location).forEach(result::add);
        }
        for (Zone zone : ambient) {
            if (zone.contains(location)) {
                result.add(zone);
            }
        }
        return result;
    }

    /**
     * @param location the location to test
     * @param tag      the tag to look for
     * @return whether any zone covering the location carries the given tag. Use this for capability checks (e.g. "is
     * this a safe zone?") where overlapping zones should all be considered, not just the highest-priority winner.
     */
    public boolean hasTagAt(@NotNull Location location, @NotNull String tag) {
        for (ZoneProvider provider : providers) {
            if (provider.zonesAt(location).anyMatch(zone -> zone.hasTag(tag))) {
                return true;
            }
        }
        for (Zone zone : ambient) {
            if (zone.hasTag(tag) && zone.contains(location)) {
                return true;
            }
        }
        return false;
    }

    private static Zone highest(Zone best, java.util.Iterator<Zone> candidates) {
        while (candidates.hasNext()) {
            final Zone candidate = candidates.next();
            if (best == null || candidate.getPriority() > best.getPriority()) {
                best = candidate;
            }
        }
        return best;
    }

    // </editor-fold>

    // <editor-fold desc="Access (hybrid: rules + event)">

    /**
     * Resolves the zone at a location and asks both halves of the rule system whether an action is allowed: first the
     * zone's attached {@link ZoneRule}s, then a {@link ZoneInteractEvent} on the bus seeded with that verdict. Handlers
     * may override the verdict.
     *
     * @param player      the acting player
     * @param location    where the action happens
     * @param interaction the kind of action
     * @param block       the block involved, if any
     * @return {@link Event.Result#DEFAULT} if no zone applies or no rule has an opinion; otherwise the verdict
     */
    public @NotNull Event.Result queryAccess(@NotNull Player player, @NotNull Location location,
                                             @NotNull ZoneInteraction interaction, @Nullable Block block) {
        return queryAccess(player, location, interaction, block, true);
    }

    /**
     * As {@link #queryAccess(Player, Location, ZoneInteraction, Block)} but lets the caller mark the query as
     * non-informing — handlers should not message the player. Use for programmatic permission checks (e.g. pre-filtering
     * tree-feller blocks) that would otherwise spam denial messages.
     */
    public @NotNull Event.Result queryAccess(@NotNull Player player, @NotNull Location location,
                                             @NotNull ZoneInteraction interaction, @Nullable Block block, boolean inform) {
        final Zone zone = getZoneAt(location);
        if (zone == null) {
            return Event.Result.DEFAULT;
        }
        return queryAccess(player, zone, location, interaction, block, inform);
    }

    public @NotNull Event.Result queryAccess(@NotNull Player player, @NotNull Zone zone, @NotNull Location location,
                                             @NotNull ZoneInteraction interaction, @Nullable Block block) {
        return queryAccess(player, zone, location, interaction, block, true);
    }

    /**
     * As {@link #queryAccess(Player, Location, ZoneInteraction, Block)} but against an explicit, already-resolved zone
     * (skips re-resolution) and with an explicit {@code inform} flag.
     *
     * @param player      the acting player
     * @param zone        the zone the action occurs in
     * @param location    where the action happens
     * @param interaction the kind of action
     * @param block       the block involved, if any
     * @param inform      whether handlers may message the player about a denial
     * @return the verdict from the zone's rules, possibly overridden by a {@link ZoneInteractEvent} handler
     */
    public @NotNull Event.Result queryAccess(@NotNull Player player, @NotNull Zone zone, @NotNull Location location,
                                             @NotNull ZoneInteraction interaction, @Nullable Block block, boolean inform) {
        final ZoneActionContext context = ZoneActionContext.builder()
                .player(player)
                .zone(zone)
                .interaction(interaction)
                .location(location)
                .block(block)
                .build();
        final Event.Result ruleResult = zone.getRules().evaluate(context);
        final ZoneInteractEvent event = new ZoneInteractEvent(player, zone, block, location, interaction, ruleResult);
        event.setInform(inform);
        event.callEvent();
        return event.getResult();
    }

    // </editor-fold>

    // <editor-fold desc="Per-player tracking">

    /**
     * @param player the player
     * @return the player's currently tracked zone, or {@code null} if they are in no zone
     */
    public @Nullable Zone getZone(@NotNull Player player) {
        return playerZones.get(player);
    }

    /**
     * @param player the player
     * @param key    a zone identity
     * @return whether the player is currently in a zone with that key
     */
    public boolean isInZone(@NotNull Player player, @NotNull Key key) {
        final Zone zone = playerZones.get(player);
        return zone != null && zone.is(key);
    }

    /**
     * Recomputes the player's zone for a location and fires {@link PlayerExitZoneEvent}/{@link PlayerEnterZoneEvent} if
     * it changed (exit fires before enter). Called by {@link ZoneListener} on movement; safe to call directly.
     *
     * @param player   the player
     * @param location their location
     */
    public void updateZone(@NotNull Player player, @NotNull Location location) {
        final Zone resolved = getZoneAt(location);
        final Zone previous = playerZones.get(player);
        if (Objects.equals(previous, resolved)) {
            return;
        }
        if (resolved == null) {
            playerZones.remove(player);
        } else {
            playerZones.put(player, resolved);
        }
        if (previous != null) {
            new PlayerExitZoneEvent(player, previous).callEvent();
        }
        if (resolved != null) {
            new PlayerEnterZoneEvent(player, resolved).callEvent();
        }
    }

    /**
     * Drops a player's tracked zone without firing events (e.g. on quit).
     *
     * @param player the player
     */
    public void clear(@NotNull Player player) {
        playerZones.remove(player);
    }

    // </editor-fold>
}
