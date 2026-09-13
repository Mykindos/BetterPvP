package me.mykindos.betterpvp.clans.world.sailing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSelector;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteWorlds;
import me.mykindos.betterpvp.core.world.site.WorldSource;
import me.mykindos.betterpvp.core.world.site.crew.Crew;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The staging world a crew occupies while a voyage is in progress. One world per voyage, cloned from a single template
 * and deleted on arrival.
 * <p>
 * This is deliberately not a {@code Site}. It cannot be travelled to directly, returned to on login, or listed in the
 * navigator, so it has no catalogue row and no instance record. One spare world is kept cloned ahead of demand so that
 * starting a voyage does not wait on a file copy.
 */
@CustomLog
@BPvPListener
@Singleton
public class Ocean implements Listener {

    /** These worlds live under the site root so that one left behind by a crash is swept up with the rest. */
    public static final String WORLD_PREFIX = SiteWorlds.worldNamePrefix("ocean");

    private final SiteWorlds worlds;
    private final SiteInstances instances;
    private final ShipService shipService;
    private final WorldContentService contentService;
    private final VoyageCues cues;

    /** The spare, cloned ahead of demand. Held as a name rather than a World so it can be claimed while unloaded. */
    private final AtomicReference<String> spare = new AtomicReference<>();

    /** Worlds handed out by {@link #claim} and not yet released. Excludes the spare, which holds no players. */
    private final Set<String> crossings = ConcurrentHashMap.newKeySet();

    @Inject
    @Config(path = "clans.voyage.ocean-template", defaultValue = "templates/islands/ocean")
    private String template;

    @Inject
    public Ocean(@NotNull SiteWorlds worlds, @NotNull SiteInstances instances, @NotNull ShipService shipService,
                 @NotNull WorldContentService contentService, @NotNull ClientManager clientManager,
                 @NotNull VoyageCues cues) {
        this.worlds = worlds;
        this.instances = instances;
        this.shipService = shipService;
        this.contentService = contentService;
        this.cues = cues;

        // Building is disabled here: the ship belongs to another player and the world is deleted on arrival.
        contentService.register(new WorldContentBinding(
                WorldSelector.prefixed(WORLD_PREFIX), () -> List.of(new OceanContent(clientManager))));
    }

    /** Clones the first spare after boot recovery finishes, so its folder sweep does not delete the new world. */
    @EventHandler
    public void onServerStart(@NotNull ServerStartEvent event) {
        instances.whenRecovered().thenRun(this::keepSpare);
    }

    /**
     * Reserves a world for a voyage, using the spare if one is ready and cloning otherwise. A replacement spare is
     * started either way.
     *
     * @return the world the crew will occupy for the voyage
     */
    public @NotNull CompletableFuture<World> claim() {
        final String claimed = spare.getAndSet(null);
        final CompletableFuture<World> opened = claimed == null
                ? worlds.open(source(), nextWorldName())
                : worlds.open(source(), claimed);

        return opened.whenComplete((world, ex) -> {
            if (world != null) {
                crossings.add(world.getName());
            }
            keepSpare();
        });
    }

    /** Releases a world once its voyage has ended. The world is deleted, so nothing in it is persisted. */
    public @NotNull CompletableFuture<Void> release(@NotNull String worldName) {
        crossings.remove(worldName);
        shipService.clearAssignments(worldName);
        return worlds.destroy(worldName).exceptionally(ex -> {
            log.warn("Could not release staging world {}", worldName, ex).submit();
            return null;
        });
    }

    /**
     * Teleports a player back onto the ship if they leave it while a voyage is running.
     * <p>
     * At a dock, leaving the ship is how a player leaves a crew. In here it cannot mean that, because the destination
     * is already chosen and the world contains no land, so without this a player would be stuck in open water until
     * the voyage ended.
     */
    @EventHandler
    public void onOverboard(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final World water = event.getTo().getWorld();
        if (!crossings.contains(water.getName())) {
            return;
        }

        final Berth deck = shipService.berths(water).stream().findFirst().orElse(null);
        if (deck == null || !deck.isCrewable() || deck.contains(event.getTo())) {
            return;
        }

        // Same world, so no passengers need detaching and Sailors is not involved.
        event.getPlayer().teleportAsync(deck.getBoard());
        cues.hauledAboard(event.getPlayer());
    }

    /**
     * Places the crew's own ship structure into the staging world.
     * <p>
     * The template declares an empty berth and no ship, so the structure pasted in is the one the crew departed from
     * and a single template serves every ship. Reloading world content rather than only pasting blocks is what brings
     * the helm and the quartermaster across: they are data points inside the structure and something has to install
     * them.
     *
     * @return the berth the ship was placed at, or empty if the template declares no berth
     */
    public @NotNull Optional<Berth> moor(@NotNull World ocean, @NotNull Crew crew) {
        final Berth mooring = shipService.berths(ocean).stream().findFirst().orElse(null);
        if (mooring == null) {
            log.warn("Staging world '{}' has no '{}' marker, so the ship cannot be placed",
                    ocean.getName(), ShipService.BERTH_POINT).submit();
            return Optional.empty();
        }

        final String vessel = vesselOf(crew);
        if (vessel.isEmpty()) {
            log.warn("Could not resolve the ship crew '{}' departed from, berth left empty", crew.getCaptain()).submit();
            return Optional.of(mooring);
        }

        shipService.assign(ocean, mooring.getId(), vessel);
        contentService.loadWorld(ocean);

        return shipService.berth(ocean, mooring.getId());
    }

    /** The ship structure the crew departed from, read from the berth the crew was formed at. */
    private @NotNull String vesselOf(@NotNull Crew crew) {
        final World origin = Bukkit.getWorld(crew.getWorldName());
        if (origin == null) {
            return "";
        }
        return shipService.berth(origin, crew.getBerthId()).map(Berth::getStructure).orElse("");
    }

    private void keepSpare() {
        if (spare.get() != null) {
            return;
        }

        final String worldName = nextWorldName();
        worlds.open(source(), worldName)
                .thenAccept(world -> spare.set(world.getName()))
                .exceptionally(ex -> {
                    log.warn("Could not prepare a spare staging world", ex).submit();
                    return null;
                });
    }

    private @NotNull WorldSource source() {
        return WorldSource.clone(template);
    }

    private @NotNull String nextWorldName() {
        return WORLD_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }
}
