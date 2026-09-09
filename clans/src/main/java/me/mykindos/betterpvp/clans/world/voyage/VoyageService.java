package me.mykindos.betterpvp.clans.world.voyage;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.content.WorldSelector;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.clans.world.island.IslandTemplate;
import me.mykindos.betterpvp.clans.world.island.IslandTemplateRegistry;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.clans.world.travel.ServerLocation;
import me.mykindos.betterpvp.clans.world.travel.TravelHistory;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.indicator.IndicatorService;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.world.mapper.RegionTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs crossings: puts a crew to sea, rolls for landfall, and sets them ashore together.
 * <p>
 * The stretch of ocean is a cloned world like any island instance, so a crew crossing has the whole instancing
 * lifecycle behind it — provisioning, the warm pool, boot recovery — without any of it being rewritten. One limbo
 * template serves every ship, because the vessel itself is pasted in from the crew's own berth.
 *
 * @see VoyageTiming for the schedule
 * @see ArrivalDistribution for which dock they land at
 */
@BPvPListener
@Singleton
@CustomLog
public class VoyageService implements Listener {

    /** Where a crew lands. Every world reachable by ship needs at least one. */
    public static final String ARRIVAL_POINT = "ship_arrive";

    /** Seconds of blindness on departure and arrival, covering the world change. */
    private static final long BLINDNESS_MILLIS = 3000L;

    /** Titles sit above ordinary chatter but below anything urgent. */
    private static final int VOYAGE_TITLE_PRIORITY = 5;

    /** Deliberately the lowest thing on the action bar: a clock should never hide a cooldown or a warning. */
    private static final int VOYAGE_ACTION_BAR_PRIORITY = 1000;

    /**
     * The island template used for the open sea. Not a destination: it is allocated by this service and must never be
     * offered as somewhere to visit.
     */
    public static final String LIMBO_TEMPLATE = "limbo";

    private final IslandTemplateRegistry templates;
    private final IslandInstanceManager instances;
    private final ShipService shipService;
    private final CrewService crewService;
    private final EffectManager effects;
    private final WorldContentService contentService;
    private final ClientManager clientManager;
    private final TravelHistory travelHistory;
    private final WorldHandler worldHandler;
    private final IndicatorService indicators;

    /** Captains whose crossing is being opened. Allocating a world is async, and two clicks would open two of them. */
    private final Set<UUID> opening = ConcurrentHashMap.newKeySet();

    /** Sailors this service is moving itself, so its own teleports are not read as somebody walking out. */
    private final Set<UUID> relocating = ConcurrentHashMap.newKeySet();

    private final List<Voyage> voyages = new CopyOnWriteArrayList<>();

    /**
     * Whether each world has anywhere to land, by world name. Cached because readiness is asked once per destination
     * every time a helm menu opens, and answering it means reading that world's Mapper file off disk.
     */
    private final Map<String, Boolean> landable = new ConcurrentHashMap<>();

    @Inject
    public VoyageService(@NotNull IslandTemplateRegistry templates, @NotNull IslandInstanceManager instances,
                         @NotNull ShipService shipService, @NotNull CrewService crewService,
                         @NotNull EffectManager effects, @NotNull WorldContentService contentService,
                         @NotNull ClientManager clientManager, @NotNull TravelHistory travelHistory,
                         @NotNull WorldHandler worldHandler, @NotNull IndicatorService indicators) {
        this.templates = templates;
        this.instances = instances;
        this.shipService = shipService;
        this.crewService = crewService;
        this.effects = effects;
        this.contentService = contentService;
        this.clientManager = clientManager;
        this.travelHistory = travelHistory;
        this.worldHandler = worldHandler;
        this.indicators = indicators;

        // The open sea is off-limits for building: it is somebody else's ship in a world that is deleted on arrival.
        contentService.register(new WorldContentBinding(
                WorldSelector.islandTemplate(LIMBO_TEMPLATE), () -> List.of(new LimboContent(clientManager))));
    }

    /** Whether crossings are possible at all — false when the limbo template is missing from config. */
    public boolean canDepart() {
        return templates.get(LIMBO_TEMPLATE).isPresent();
    }

    /**
     * Whether a crew could be set ashore in this world — that is, whether it has any {@link #ARRIVAL_POINT} at all.
     * <p>
     * Asked before a course can be set, not only when the crew gets there. A world reachable by ship but with no dock
     * built yet is the one case the crossing itself cannot recover from: by the time it is discovered the crew has
     * spent the whole voyage at sea and their ocean is about to be deleted.
     */
    public boolean canLand(@NotNull String worldName) {
        return landable.computeIfAbsent(worldName, name -> {
            final World world = Bukkit.getWorld(name);
            return world != null && !arrivals(world).isEmpty();
        });
    }

    /**
     * Forgets what was known about a world's landings. A world that has just loaded may not have been readable before,
     * and one being unloaded must not answer for whatever takes its name next — instance worlds recycle names.
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        landable.remove(event.getWorld().getName());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        landable.remove(event.getWorld().getName());
    }

    /**
     * Puts {@code crew} to sea. The roster freezes here: from this point nobody joins, nobody leaves, and stepping off
     * the deck means nothing, because the destination is already committed.
     */
    public @NotNull CompletableFuture<Boolean> begin(@NotNull Crew crew, @NotNull Landfall destination) {
        // Opening a crossing waits on a world being cloned, so a second click lands before the first has anything to
        // show for itself. Without this the captain gets two oceans and their crew is split between them.
        if (!opening.add(crew.getCaptain())) {
            return CompletableFuture.completedFuture(false);
        }

        final Optional<IslandTemplate> limboTemplate = templates.get(LIMBO_TEMPLATE);
        if (limboTemplate.isEmpty()) {
            log.warn("No island template '{}' - crossings cannot run", LIMBO_TEMPLATE).submit();
            opening.remove(crew.getCaptain());
            return CompletableFuture.completedFuture(false);
        }

        return instances.allocate(limboTemplate.get()).thenApply(limbo -> {
            crewService.depart(crew);
            final Voyage voyage = new Voyage(crew, destination, destination.timing(), limbo,
                    System.currentTimeMillis());
            voyages.add(voyage);

            try {
                // A crossing that could not put its crew to sea must not stay on the books. Left rolling, it would sit
                // there for a minute and then teleport people to a destination from wherever they had wandered off to.
                if (!setSail(voyage)) {
                    abandon(voyage);
                    return false;
                }
                return true;
            } catch (RuntimeException exception) {
                // Anything thrown past here would leave the crew flagged as sailing with no crossing to end it: the
                // helm refuses them, the hull will not re-crew them, and nothing else ever puts them back ashore.
                log.error("Could not put crew {} to sea", crew.getCaptain(), exception).submit();
                abandon(voyage);
                return false;
            } finally {
                opening.remove(crew.getCaptain());
            }
        }).exceptionally(throwable -> {
            opening.remove(crew.getCaptain());
            log.error("Could not open a crossing for {}", crew.getCaptain(), throwable).submit();
            aboard(crew).forEach(sailor ->
                    UtilMessage.message(sailor, "clans.prefix.ship", "clans.voyage.failed"));
            return false;
        });
    }

    /**
     * Moves the crew onto their ship, out in open water.
     * <p>
     * The limbo template declares a mooring but no vessel, so the hull put there is the one they boarded at the dock —
     * one template serves the whole fleet.
     */
    private boolean setSail(@NotNull Voyage voyage) {
        final World limbo = Bukkit.getWorld(voyage.getLimbo().getWorldName());
        if (limbo == null) {
            log.warn("Limbo world '{}' vanished before the crew could board", voyage.getLimbo().getWorldName()).submit();
            return false;
        }

        final Optional<Berth> moored = moorTheirVessel(limbo, voyage.getCrew());
        if (moored.isEmpty()) {
            return false;
        }

        final Location deck = moored.get().getBoard();
        final List<Player> sailors = aboard(voyage.getCrew());

        // Before the teleports, not after them. The reaper releases any instance that reports itself empty, and a
        // warm-pool ocean is already past its grace period the moment it is claimed - so a crossing that waits for its
        // boarding teleport to land can have the world destroyed out from under it in the gap.
        sailors.forEach(sailor -> instances.enter(voyage.getLimbo(), sailor));

        for (Player sailor : sailors) {
            relocate(sailor, deck);
            castOffCues(sailor, voyage.getDestination());
        }
        return true;
    }

    /** Calls a crossing off before it started, releasing the ocean and letting the crew carry on where they are. */
    private void abandon(@NotNull Voyage voyage) {
        voyages.remove(voyage);
        aboard(voyage.getCrew()).forEach(sailor ->
                UtilMessage.message(sailor, "clans.prefix.ship", "clans.voyage.failed"));

        crewService.disband(voyage.getCrew());
        shipService.clearAssignments(voyage.getLimbo().getWorldName());
        instances.release(voyage.getLimbo().getId(), true);
    }

    /**
     * Puts the crew's own ship in their patch of ocean.
     * <p>
     * The limbo template names no vessel — its berth is an empty mooring — so the hull is whichever one they were
     * standing on when they set the course. Reloading the world's content rather than only pasting blocks is what
     * brings the helm and the quartermaster with it: they are ordinary data-points inside the structure, and something
     * has to install them.
     *
     * @return the moored berth, or empty if their vessel could not be identified or the mooring is missing
     */
    private @NotNull Optional<Berth> moorTheirVessel(@NotNull World limbo, @NotNull Crew crew) {
        final Berth mooring = shipService.berths(limbo).stream().findFirst().orElse(null);
        if (mooring == null) {
            log.warn("Limbo world '{}' has no '{}' marker - the crew has nothing to stand on",
                    limbo.getName(), ShipService.BERTH_POINT).submit();
            return Optional.empty();
        }

        final String vessel = vesselOf(crew);
        if (vessel.isEmpty()) {
            log.warn("Could not tell which ship crew '{}' sailed from - mooring left empty", crew.getCaptain()).submit();
            return Optional.of(mooring);
        }

        shipService.assign(limbo, mooring.getId(), vessel);
        contentService.loadWorld(limbo);

        return shipService.berth(limbo, mooring.getId());
    }

    /** The structure the crew boarded at the dock, read back off the berth they mustered at. */
    private @NotNull String vesselOf(@NotNull Crew crew) {
        final World origin = Bukkit.getWorld(crew.getWorldName());
        if (origin == null) {
            return "";
        }
        return shipService.berth(origin, crew.getBerthId()).map(Berth::getStructure).orElse("");
    }

    private void castOffCues(@NotNull Player sailor, @NotNull Landfall destination) {
        effects.addEffect(sailor, EffectTypes.BLINDNESS, 1, BLINDNESS_MILLIS);
        title(sailor, VOYAGE_TITLE_PRIORITY,
                Translations.component("clans.voyage.title.departing"),
                destination.displayName().color(NamedTextColor.GRAY));

        new SoundEffect(Sound.ENTITY_BOAT_PADDLE_WATER, 0.7f, 1.0f).play(sailor);
        new SoundEffect(Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 0.6f, 0.8f).play(sailor);
        new SoundEffect(Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.5f).play(sailor);
    }

    /**
     * One roll per crossing, ten seconds apart. Per crossing rather than per sailor, so a crew always sights land at
     * the same moment and lands together.
     */
    @UpdateEvent(delay = 10_000)
    public void rollForLandfall() {
        final long now = System.currentTimeMillis();
        for (Voyage voyage : voyages) {
            if (voyage.arrives(now, ThreadLocalRandom.current().nextDouble())) {
                makeLandfall(voyage);
            }
        }
    }

    /** Hands the crew to their destination to be set ashore, and gives the ocean back once they are off it. */
    private void makeLandfall(@NotNull Voyage voyage) {
        voyages.remove(voyage);

        final List<Player> sailors = stillAtSea(voyage);
        sailors.forEach(this::readyToDisembark);

        landfall(voyage, sailors).whenComplete((landed, error) -> {
            if (error != null) {
                log.error("Landfall for crew {} failed", voyage.getCrew().getCaptain(), error).submit();
            }

            crewService.disband(voyage.getCrew());

            // Empty the mooring before the world goes, so a recycled instance never starts with the last crew's ship.
            shipService.clearAssignments(voyage.getLimbo().getWorldName());
            instances.release(voyage.getLimbo().getId(), true);
        });
    }

    /**
     * Runs the arrival: the destination puts them ashore, or the ship turns around.
     * <p>
     * The ocean is not released until this completes. Destroying an instance clears whoever is still inside it to the
     * server's fallback world, and that would beat a landing still in flight — dropping the crew at spawn instead of
     * at the place they spent the whole crossing sailing to.
     *
     * @return completes once every sailor is off the ocean, however they left it
     */
    private @NotNull CompletableFuture<Boolean> landfall(@NotNull Voyage voyage, @NotNull List<Player> sailors) {
        final CompletableFuture<Boolean> ashore;
        try {
            ashore = voyage.getDestination().setAshore(sailors);
        } catch (RuntimeException exception) {
            // Thrown rather than returned, so nothing downstream would ever see it - and the ocean these people are
            // standing on is released by this crossing or by nothing at all.
            return CompletableFuture.failedFuture(exception);
        }

        return ashore.thenCompose(landed -> {
            if (Boolean.TRUE.equals(landed)) {
                sailors.forEach(sailor -> landfallCues(sailor, voyage));
                return CompletableFuture.completedFuture(true);
            }

            // Nowhere to land them, and the ocean under their feet is about to be deleted. Turning the ship around is
            // the only honest ending: left standing there they would be flung to whatever world the server unloads
            // into, which is nowhere they chose to be.
            final List<CompletableFuture<Boolean>> back = new ArrayList<>();
            for (Player sailor : sailors) {
                UtilMessage.message(sailor, "clans.prefix.ship", "clans.voyage.no-harbour");
                back.add(relocate(sailor, turnBack(sailor)));
            }
            return CompletableFuture.allOf(back.toArray(CompletableFuture[]::new)).thenApply(ignored -> false);
        });
    }

    /**
     * Gets a sailor ready to be moved off the ocean, whoever ends up moving them.
     * <p>
     * Paper refuses to move anybody carrying passengers to another world, and a captain carries one without ever being
     * told: their crew marker is mounted on them. Done here rather than only in {@link #relocate} because a destination
     * may land its own travellers, and a marker left up would have the teleport quietly report false instead.
     */
    private void readyToDisembark(@NotNull Player sailor) {
        indicators.detach(sailor);
        sailor.eject();
    }

    /**
     * Moves a sailor ashore at the end of a crossing. For a {@link Landfall} that lands its crew at a spot it already
     * knows, rather than by handing them to something that does its own travelling.
     */
    public @NotNull CompletableFuture<Boolean> putAshore(@NotNull Player sailor, @NotNull Location landing) {
        return relocate(sailor, landing);
    }

    /** Where a sailor goes when the crossing has nowhere to land: back where they set out from, else spawn. */
    private @NotNull Location turnBack(@NotNull Player sailor) {
        return travelHistory.origin(sailor)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation);
    }

    /**
     * The sailors still on the crossing's own stretch of ocean.
     * <p>
     * Anyone who left — by command, by an admin, by anything that moved them off the ship — has already ended their
     * crossing, and hauling them to the destination minutes later would teleport them out of whatever they went off to
     * do.
     */
    private @NotNull List<Player> stillAtSea(@NotNull Voyage voyage) {
        final String limboWorld = voyage.getLimbo().getWorldName();
        return aboard(voyage.getCrew()).stream()
                .filter(sailor -> sailor.getWorld().getName().equals(limboWorld))
                .toList();
    }

    /**
     * The dock a crew lands at in {@code world}, picked once for the whole crew so they arrive together.
     *
     * @return the landing, or empty if the world has nowhere to put anybody ashore
     */
    public @NotNull Optional<Location> landingIn(@NotNull World world, @NotNull ArrivalDistribution distribution) {
        final List<ArrivalPoint> docks = arrivals(world);
        if (docks.isEmpty()) {
            log.warn("World '{}' has no '{}' marker - crews cannot land there",
                    world.getName(), ARRIVAL_POINT).submit();
            return Optional.empty();
        }

        final List<String> names = docks.stream().map(ArrivalPoint::getName).toList();
        final int chosen = Math.floorMod(distribution.select(names), docks.size());
        return Optional.of(docks.get(chosen).getLocation());
    }

    /**
     * Every place a crew can be set ashore in a world, in map order.
     * <p>
     * Public because landing is not only the end of a crossing: anything that puts people on a shore reads the same
     * markers, and two readings of the same file would drift apart.
     */
    public @NotNull List<ArrivalPoint> arrivals(@NotNull World world) {
        final List<ArrivalPoint> docks = new ArrayList<>();
        MapperHelper.readRegions(world).ifPresent(regions -> {
            for (Region region : regions) {
                if (ARRIVAL_POINT.equalsIgnoreCase(region.getName()) && region instanceof PerspectiveRegion dock) {
                    dock.setWorld(world);
                    docks.add(new ArrivalPoint(RegionTags.of(dock).getString("name", dock.getName()),
                            dock.getLocation()));
                }
            }
        });
        return docks;
    }

    private void landfallCues(@NotNull Player sailor, @NotNull Voyage voyage) {
        effects.addEffect(sailor, EffectTypes.BLINDNESS, 1, BLINDNESS_MILLIS);
        title(sailor, VOYAGE_TITLE_PRIORITY,
                voyage.getDestination().displayName().color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Translations.component("clans.voyage.title.landfall").color(NamedTextColor.GRAY));

        new SoundEffect(Sound.ENTITY_BOAT_PADDLE_LAND, 0.8f, 1.0f).play(sailor);
        new SoundEffect(Sound.AMBIENT_UNDERWATER_EXIT, 1.0f, 0.6f).play(sailor);
    }

    /**
     * Puts anyone who goes over the side back on deck.
     * <p>
     * At the dock, stepping off the hull is how you leave a crew. Out here it cannot mean that — the destination is
     * committed and there is nothing around but open water — so a mistimed jump would leave somebody treading water in
     * a world with no land until the crossing ended. Fishing them out is the kinder reading of the same gesture.
     */
    @EventHandler
    public void onOverboard(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final Voyage voyage = voyageOf(event.getPlayer().getUniqueId()).orElse(null);
        if (voyage == null) {
            return;
        }

        final World limbo = Bukkit.getWorld(voyage.getLimbo().getWorldName());
        if (limbo == null || !limbo.equals(event.getTo().getWorld())) {
            return;
        }

        final Berth deck = shipService.berths(limbo).stream().findFirst().orElse(null);
        if (deck == null || !deck.isCrewable() || deck.contains(event.getTo())) {
            return;
        }

        relocate(event.getPlayer(), deck.getBoard());
        UtilMessage.message(event.getPlayer(), "clans.prefix.ship", "clans.voyage.hauled-aboard");
        new SoundEffect(Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f).play(event.getPlayer());
    }

    /** The crossing {@code player} is on, if any. */
    public @NotNull Optional<Voyage> voyageOf(@NotNull UUID player) {
        return voyages.stream().filter(voyage -> voyage.getCrew().has(player)).findFirst();
    }

    /**
     * A running clock while at sea, on the action bar.
     * <p>
     * Pushed at the lowest priority so anything with something to say — a cooldown, a warning — sits above it. Landfall
     * is a roll rather than a countdown, so this reports how long they have been sailing rather than promising a time
     * it cannot know.
     */
    @UpdateEvent(delay = 1000)
    public void showVoyageClock() {
        final long now = System.currentTimeMillis();
        for (Voyage voyage : voyages) {
            final Component destination = voyage.getDestination().displayName().color(NamedTextColor.AQUA);
            final Component elapsed = Component.text(clock(voyage.elapsedSeconds(now)), NamedTextColor.WHITE);

            for (Player sailor : stillAtSea(voyage)) {
                gamer(sailor).ifPresent(gamer -> gamer.getActionBar().add(VOYAGE_ACTION_BAR_PRIORITY,
                        new TimedComponent(1.5, false,
                                gmr -> Translations.component("clans.voyage.actionbar", destination, elapsed))));
            }
        }
    }

    private static @NotNull String clock(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Ends a sailor's crossing when something moves them off the ocean — a home command, an admin, anything.
     * <p>
     * Their own arrival is excluded: this service teleports the crew itself, and reading that as somebody leaving would
     * cancel every crossing at the moment it succeeded.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportOut(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (relocating.contains(player.getUniqueId()) || event.getTo() == null) {
            return;
        }

        final Voyage voyage = voyageOf(player.getUniqueId()).orElse(null);
        if (voyage == null || !voyage.getLimbo().getWorldName().equals(event.getFrom().getWorld().getName())) {
            return;
        }
        if (voyage.getLimbo().getWorldName().equals(event.getTo().getWorld().getName())) {
            return; // still aboard, just moved
        }

        // Leaving the ocean ends it for them alone; the rest of the crew sails on.
        crewService.leave(player.getUniqueId());
        UtilMessage.message(player, "clans.prefix.ship", "clans.voyage.left");
    }

    /**
     * Moves a sailor, marking the teleport as ours so {@link #onTeleportOut} does not read it as them walking out.
     * <p>
     * A refused move is reported rather than swallowed. Every visible symptom of a crossing going wrong is somebody
     * standing where they were, and without this the one event that says so is discarded.
     */
    private @NotNull CompletableFuture<Boolean> relocate(@NotNull Player sailor, @NotNull Location destination) {
        // Paper refuses to move anybody carrying passengers to another world, and a captain carries one without ever
        // being told: their crew marker is mounted on them. Every leg of a crossing changes world, so the marker has to
        // come down first or the teleport simply reports false and the ship never leaves.
        indicators.detach(sailor);
        sailor.eject();

        relocating.add(sailor.getUniqueId());
        return sailor.teleportAsync(destination).whenComplete((moved, error) -> {
            relocating.remove(sailor.getUniqueId());
            if (error != null) {
                log.warn("Could not move {} to {}", sailor.getName(), destination, error).submit();
            } else if (!Boolean.TRUE.equals(moved)) {
                log.warn("Move of {} to {} was refused", sailor.getName(), destination).submit();
            }
        });
    }

    private void title(@NotNull Player sailor, int priority, @NotNull Component heading, @NotNull Component sub) {
        gamer(sailor).ifPresent(gamer -> gamer.getTitleQueue().add(priority,
                new TitleComponent(0.2, 1.6, 0.4, false, gmr -> heading, gmr -> sub)));
    }

    private @NotNull Optional<Gamer> gamer(@NotNull Player player) {
        return Optional.ofNullable(clientManager.search().online(player)).map(client -> client.getGamer());
    }

    /** The crew members currently online. Offline sailors simply miss the crossing rather than holding it up. */
    private @NotNull List<Player> aboard(@NotNull Crew crew) {
        final List<Player> present = new ArrayList<>();
        for (UUID member : crew.roster()) {
            final Player online = Bukkit.getPlayer(member);
            if (online != null) {
                present.add(online);
            }
        }
        return present;
    }
}
