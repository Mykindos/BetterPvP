package me.mykindos.betterpvp.clans.world.sailing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSelector;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteWorlds;
import me.mykindos.betterpvp.core.world.site.WorldSource;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The open water a crew crosses. A private world per voyage, cloned from one template and deleted when they land.
 * <p>
 * Not a site: you cannot travel to it, be returned to it, or see it in a navigator. It is a staging world the sailing
 * package makes for itself, which is why it holds no catalogue row and no instance record. One spare is kept ready so
 * casting off does not wait on a world being copied.
 */
@CustomLog
@BPvPListener
@Singleton
public class Ocean implements Listener {

    /** Ocean worlds sit under the site root so a crash-left one is swept with everything else disposable. */
    public static final String WORLD_PREFIX = SiteWorlds.worldNamePrefix("ocean");

    private final SiteWorlds worlds;
    private final SiteInstances instances;
    private final ShipService shipService;
    private final WorldContentService contentService;

    /** The spare, cloned ahead of demand. Held as a name rather than a world so it can be claimed without loading. */
    private final AtomicReference<String> spare = new AtomicReference<>();

    @Inject
    @Config(path = "clans.voyage.ocean-template", defaultValue = "templates/islands/limbo")
    private String template;

    @Inject
    public Ocean(@NotNull SiteWorlds worlds, @NotNull SiteInstances instances, @NotNull ShipService shipService,
                 @NotNull WorldContentService contentService, @NotNull ClientManager clientManager) {
        this.worlds = worlds;
        this.instances = instances;
        this.shipService = shipService;
        this.contentService = contentService;

        // The open sea is off-limits for building: it is somebody else's ship in a world deleted on arrival.
        contentService.register(new WorldContentBinding(
                WorldSelector.prefixed(WORLD_PREFIX), () -> List.of(new OceanContent(clientManager))));
    }

    /** Clones the first spare once boot recovery has finished sweeping folders it might otherwise delete. */
    @EventHandler
    public void onServerStart(@NotNull ServerStartEvent event) {
        instances.whenRecovered().thenRun(this::keepSpare);
    }

    /**
     * Takes a stretch of ocean for a crossing, using the spare if one is ready and cloning otherwise. A replacement is
     * started either way.
     *
     * @return the world the crew will sail on
     */
    public @NotNull CompletableFuture<World> claim() {
        final String claimed = spare.getAndSet(null);
        final CompletableFuture<World> opened = claimed == null
                ? worlds.open(source(), nextWorldName())
                : worlds.open(source(), claimed);

        return opened.whenComplete((world, ex) -> keepSpare());
    }

    /** Gives a stretch of ocean back. The world is deleted: nothing on it outlives the crossing. */
    public @NotNull CompletableFuture<Void> release(@NotNull String worldName) {
        shipService.clearAssignments(worldName);
        return worlds.destroy(worldName).exceptionally(ex -> {
            log.warn("Could not release ocean world {}", worldName, ex).submit();
            return null;
        });
    }

    /**
     * Puts the crew's own ship in their patch of ocean.
     * <p>
     * The template names no vessel, only an empty mooring, so the hull is whichever one they were standing on when
     * they set the course. Reloading the world's content rather than only pasting blocks is what brings the helm and
     * the quartermaster with it: they are ordinary data-points inside the structure, and something has to install them.
     *
     * @return the moored berth, or empty if the template has no mooring at all
     */
    public @NotNull Optional<Berth> moor(@NotNull World ocean, @NotNull Crew crew) {
        final Berth mooring = shipService.berths(ocean).stream().findFirst().orElse(null);
        if (mooring == null) {
            log.warn("Ocean world '{}' has no '{}' marker - the crew has nothing to stand on",
                    ocean.getName(), ShipService.BERTH_POINT).submit();
            return Optional.empty();
        }

        final String vessel = vesselOf(crew);
        if (vessel.isEmpty()) {
            log.warn("Could not tell which ship crew '{}' sailed from - mooring left empty", crew.getCaptain()).submit();
            return Optional.of(mooring);
        }

        shipService.assign(ocean, mooring.getId(), vessel);
        contentService.loadWorld(ocean);

        return shipService.berth(ocean, mooring.getId());
    }

    /** The structure the crew boarded at the dock, read back off the berth they mustered at. */
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
                    log.warn("Could not prepare a spare ocean", ex).submit();
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
