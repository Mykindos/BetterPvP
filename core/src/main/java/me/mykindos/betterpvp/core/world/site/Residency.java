package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.travel.ServerLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a player is put when they log in, and where anything else sends them when it has nowhere better.
 * <p>
 * Two records back this, both in {@code site_residency}. The <i>residence</i> is the instance they were last in and
 * the spot they were standing on. The <i>anchor</i> is the last site they were in that will have them back, which is
 * not the same thing: an island released the moment its last visitor leaves cannot be somewhere a player returns to,
 * so it never becomes an anchor and the site before it stays.
 * <p>
 * Both are read once per session, while the connection is still being configured, and held for as long as the player
 * is online. Everything that asks afterwards is on the main thread and gets the copy rather than a query.
 */
@BPvPListener
@Singleton
@CustomLog
public class Residency implements Listener {

    private final Core core;
    private final ClientManager clientManager;
    private final ResidencyStore store;
    private final SiteRegistry registry;
    private final SiteInstances instances;
    private final Placement placement;
    private final WorldHandler worldHandler;

    /** What each online player's rows say, keyed by player rather than client so a lookup needs no client. */
    private final Map<UUID, Map<ResidencyStore.Kind, Residence>> records = new ConcurrentHashMap<>();

    @Inject
    public Residency(@NotNull Core core, @NotNull ClientManager clientManager, @NotNull ResidencyStore store,
                     @NotNull SiteRegistry registry, @NotNull SiteInstances instances, @NotNull Placement placement,
                     @NotNull WorldHandler worldHandler) {
        this.core = core;
        this.clientManager = clientManager;
        this.store = store;
        this.registry = registry;
        this.instances = instances;
        this.placement = placement;
        this.worldHandler = worldHandler;
    }

    /**
     * Where to send a player who has to be moved and has nowhere in particular to go, such as one leaving a site or
     * one whose voyage found no destination. Their anchor if it is reachable from this server, otherwise spawn.
     */
    public @NotNull Location fallback(@NotNull Player player) {
        return record(player.getUniqueId(), ResidencyStore.Kind.ANCHOR)
                .map(Residence::getLocation)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation);
    }

    /**
     * Chooses the login position while the connection is still being configured. Doing it here rather than after join
     * means no teleport is involved, and anything that deliberately relocates a player during join still takes
     * precedence.
     */
    @EventHandler
    public void onSpawnLocation(@NotNull AsyncPlayerSpawnLocationEvent event) {
        final UUID uuid = event.getConnection().getProfile().getId();
        if (uuid != null) {
            loginLocation(uuid).ifPresent(event::setSpawnLocation);
        }
    }

    /**
     * Handles the logins {@link #onSpawnLocation} could not, being the ones that need a world opened first. Opening
     * one takes long enough that the player is let into the game meanwhile and moved when it is ready.
     */
    @EventHandler
    public void onJoin(@NotNull ClientJoinEvent event) {
        final Player player = event.getPlayer();
        if (!records.containsKey(player.getUniqueId())) {
            // Nothing read the rows for this player, which happens to anyone already online when the server finishes
            // loading. Off the main thread, since this one is not in a hurry.
            final long client = event.getClient().getId();
            UtilServer.runTaskAsync(core, () -> records.put(player.getUniqueId(), store.load(client)));
            return;
        }

        record(player.getUniqueId(), ResidencyStore.Kind.RESIDENCE)
                .filter(this::needsWorldOpened)
                .ifPresent(residence -> reopen(player, residence));
    }

    /**
     * Records where a player was when they left. Quitting outside any site clears the residence instead, because their
     * own saved position is then correct and overriding it on the next login would move them for no reason.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull ClientQuitEvent event) {
        final Player player = event.getPlayer();
        final long client = event.getClient().getId();
        records.remove(player.getUniqueId());

        final Optional<SiteInstance> instance = instances.byWorld(player.getWorld().getName());
        if (instance.isEmpty()) {
            store.clear(client, ResidencyStore.Kind.RESIDENCE);
            return;
        }

        final Residence residence = Residence.of(instance.get(), player.getLocation());
        store.save(client, ResidencyStore.Kind.RESIDENCE, residence);
        if (isAnchorable(instance.get())) {
            store.save(client, ResidencyStore.Kind.ANCHOR, residence);
        }
    }

    /**
     * Moves the anchor when a player leaves an anchorable site, recording the spot they left from rather than the one
     * they arrived at.
     * <p>
     * The anchor is one record and not a stack, so this replaces it outright. Gating it on the site being anchorable
     * is what makes it the last real place they were: Spawn to woodcutting to mining leaves the anchor at Spawn, so
     * both islands can be released without anybody needing to be found a new home.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        final Location from = event.getFrom();
        if (event.getTo() == null || event.getTo().getWorld().equals(from.getWorld())) {
            return;
        }

        instances.byWorld(from.getWorld().getName())
                .filter(this::isAnchorable)
                .ifPresent(instance -> anchor(event.getPlayer(), Residence.of(instance, from)));
    }

    private void anchor(@NotNull Player player, @NotNull Residence residence) {
        records.computeIfAbsent(player.getUniqueId(), key -> new EnumMap<>(ResidencyStore.Kind.class))
                .put(ResidencyStore.Kind.ANCHOR, residence);

        clientManager.getStoredExact(player.getUniqueId())
                .ifPresent(client -> store.save(client.getId(), ResidencyStore.Kind.ANCHOR, residence));
    }

    /**
     * Reads a player's rows and works out where they materialise, or empty to leave them wherever the server would
     * have put them.
     * <p>
     * A player with no residence is left alone, which is everybody who last logged out somewhere that is not a site.
     * One who has a residence is placed by this or by nothing, so an instance that is gone always resolves to
     * somewhere rather than leaving them standing in a world that no longer belongs to anybody.
     */
    @NotNull Optional<Location> loginLocation(@NotNull UUID player) {
        final Optional<Client> client = clientManager.getStoredExact(player);
        if (client.isEmpty()) {
            log.warn("No client loaded for {} at spawn selection, leaving them where the server put them", player).submit();
            return Optional.empty();
        }

        records.put(player, store.load(client.get().getId()));

        final Optional<Residence> residence = record(player, ResidencyStore.Kind.RESIDENCE);
        if (residence.isEmpty()) {
            return Optional.empty();
        }

        final Optional<Location> resumed = resume(residence.get());
        if (resumed.isPresent() || needsWorldOpened(residence.get())) {
            return resumed;
        }

        return record(player, ResidencyStore.Kind.ANCHOR)
                .map(Residence::getLocation)
                .flatMap(ServerLocation::toLocation)
                .or(() -> Optional.of(worldHandler.getSpawnLocation()));
    }

    /** The spot in the instance a player left, if that instance is loaded and its site takes people back. */
    private @NotNull Optional<Location> resume(@NotNull Residence residence) {
        final Optional<Site> site = registry.get(residence.getSite().getSiteId());
        if (site.isEmpty() || site.get().getPolicy().getRejoin() != SitePolicy.Rejoin.RESUME) {
            return Optional.empty();
        }

        return instances.find(residence.getInstanceId())
                .filter(instance -> instance.getState() == SiteInstance.State.READY)
                .flatMap(instance -> spotIn(instance, residence, site.get().getPolicy().getRejoinAt()));
    }

    /** Whether the site would take the player back, but only once a world has been opened for them. */
    private boolean needsWorldOpened(@NotNull Residence residence) {
        final Optional<Site> site = registry.get(residence.getSite().getSiteId());
        if (site.isEmpty()) {
            return false;
        }

        final SitePolicy.Rejoin rejoin = site.get().getPolicy().getRejoin();
        if (rejoin == SitePolicy.Rejoin.REINSTANCE) {
            return true;
        }

        return rejoin == SitePolicy.Rejoin.RESUME && instances.find(residence.getInstanceId())
                .filter(instance -> instance.getState() == SiteInstance.State.DORMANT)
                .isPresent();
    }

    private @NotNull Optional<Location> spotIn(@NotNull SiteInstance instance, @NotNull Residence residence,
                                               @NotNull SitePolicy.RejoinAt rejoinAt) {
        final World world = Bukkit.getWorld(instance.getWorldName());
        if (world == null) {
            return Optional.empty();
        }

        return rejoinAt == SitePolicy.RejoinAt.EXACT
                ? residence.getLocation().toLocation()
                : Optional.of(world.getSpawnLocation());
    }

    /** Opens a world for the site the player left and puts them in it, falling back to their anchor if that fails. */
    private void reopen(@NotNull Player player, @NotNull Residence residence) {
        placement.locate(residence.getSite(), Party.solo(player.getUniqueId()))
                .thenCompose(handle -> placement.send(player, handle).thenApply(placed -> {
                    if (Boolean.TRUE.equals(placed)) {
                        settle(player, residence, handle);
                    }
                    return placed;
                }))
                .exceptionally(error -> {
                    log.warn("Could not return {} to site {}", player.getName(), residence.getSite(), error).submit();
                    return false;
                })
                .thenAccept(placed -> {
                    if (!Boolean.TRUE.equals(placed)) {
                        UtilServer.runTask(core, () -> player.teleportAsync(fallback(player)));
                    }
                });
    }

    /**
     * Moves a player from the site's arrival point to the spot they logged out on, for a site that returns people to
     * where they were.
     * <p>
     * The world comes from the handle rather than from the stored location, because that location was read while its
     * world was closed and so has no world behind it. The coordinates only mean anything in the instance they were
     * recorded in, so a site that answered with a different one leaves the player at its arrival point.
     */
    private void settle(@NotNull Player player, @NotNull Residence residence, @NotNull SiteHandle handle) {
        final boolean exact = registry.get(residence.getSite().getSiteId())
                .map(site -> site.getPolicy().getRejoinAt() == SitePolicy.RejoinAt.EXACT)
                .orElse(false);
        if (!exact || !handle.getInstanceId().equals(residence.getInstanceId())) {
            return;
        }

        final World world = Bukkit.getWorld(handle.getWorld());
        if (world == null) {
            return;
        }

        final Location spot = residence.getLocation().getLocation();
        UtilServer.runTask(core, () -> player.teleportAsync(new Location(world, spot.getX(), spot.getY(), spot.getZ(),
                spot.getYaw(), spot.getPitch())));
    }

    private boolean isAnchorable(@NotNull SiteInstance instance) {
        return registry.get(instance.getKey().getSiteId())
                .map(site -> site.getPolicy().isAnchorable())
                .orElse(false);
    }

    private @NotNull Optional<Residence> record(@NotNull UUID player, @NotNull ResidencyStore.Kind kind) {
        return Optional.ofNullable(records.get(player)).map(held -> held.get(kind));
    }
}
