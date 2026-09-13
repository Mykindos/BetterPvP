package me.mykindos.betterpvp.clans.world.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.site.crew.Crew;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import me.mykindos.betterpvp.clans.world.discovery.hazard.HazardService;
import me.mykindos.betterpvp.clans.world.discovery.sighting.SightingService;
import me.mykindos.betterpvp.clans.world.discovery.wind.WindService;
import me.mykindos.betterpvp.clans.world.island.IslandOffer;
import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import me.mykindos.betterpvp.clans.world.island.IslandInstanceManager;
import me.mykindos.betterpvp.clans.world.island.IslandTemplate;
import me.mykindos.betterpvp.clans.world.island.IslandTemplateRegistry;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.clans.world.ship.ShipService;
import me.mykindos.betterpvp.clans.world.voyage.ArrivalDistribution;
import me.mykindos.betterpvp.clans.world.voyage.ArrivalPoint;
import me.mykindos.betterpvp.clans.world.voyage.VoyageService;
import me.mykindos.betterpvp.clans.world.travel.ServerLocation;
import me.mykindos.betterpvp.clans.world.travel.TravelHistory;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.indicator.IndicatorService;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.world.WorldHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs expeditions: puts a crew to sea aboard their own vessel and sails it, tick by tick, until they come about.
 * <p>
 * Nothing in the world moves. The hull is moored in a cloned limbo world for the whole trip while a
 * {@link ShipDynamics} and an {@link Ocean} carry a virtual ship across an imaginary plane — so the sea is unbounded
 * and costs one world per crew rather than one world per mile.
 *
 * @see Expedition for the per-crew state
 * @see ShipFrame for the geometry that puts the virtual sea back beside the real hull
 */
@BPvPListener
@Singleton
@CustomLog
public class ExpeditionService implements Listener {

    /** Seconds of blindness on departure and on the return, covering the world change. */
    private static final long BLINDNESS_MILLIS = 3000L;

    /** Titles sit above ordinary chatter but below anything urgent. */
    private static final int EXPEDITION_TITLE_PRIORITY = 5;

    /** Deliberately the lowest thing on the action bar: a speed readout should never hide a cooldown or a warning. */
    private static final int EXPEDITION_ACTION_BAR_PRIORITY = 1000;

    /**
     * The island template used for the open sea. Not a destination: it is allocated by this service and must never be
     * offered as somewhere to visit.
     */
    @Inject
    @Config(path = "clans.discovery.limbo-template", defaultValue = "limbo")
    private String limboTemplate;

    @Inject
    @Config(path = "clans.discovery.base-speed", defaultValue = "2.0")
    private double baseSpeed;

    @Inject
    @Config(path = "clans.discovery.rudder-in-rate", defaultValue = "0.9")
    private double rudderInRate;

    @Inject
    @Config(path = "clans.discovery.rudder-out-rate", defaultValue = "0.12")
    private double rudderOutRate;

    @Inject
    @Config(path = "clans.discovery.rudder-resistance", defaultValue = "0.65")
    private double rudderResistance;

    @Inject
    @Config(path = "clans.discovery.max-yaw-rate", defaultValue = "14.0")
    private double maxYawRate;

    @Inject
    @Config(path = "clans.discovery.hull-response", defaultValue = "1.6")
    private double hullResponse;

    @Inject
    @Config(path = "clans.discovery.turn-drag", defaultValue = "0.35")
    private double turnDrag;

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
    private final SteeringService steering;
    private final HazardService hazards;
    private final SightingService sightings;
    private final WindService wind;
    private final ContestedLandingPolicy contests;
    private final VoyageService voyageService;

    /** Captains whose expedition is being opened. Allocating a world is async, and two clicks would open two of them. */
    private final Set<UUID> opening = ConcurrentHashMap.newKeySet();

    /** Sailors this service is moving itself, so its own teleports are not read as somebody walking out. */
    private final Set<UUID> relocating = ConcurrentHashMap.newKeySet();

    private final Map<UUID, Expedition> expeditions = new ConcurrentHashMap<>();

    /** When the simulation last stepped, so a step is worth the time that actually passed. */
    private long lastTickAt = System.currentTimeMillis();

    @Inject
    public ExpeditionService(@NotNull IslandTemplateRegistry templates, @NotNull IslandInstanceManager instances,
                             @NotNull ShipService shipService, @NotNull CrewService crewService,
                             @NotNull EffectManager effects, @NotNull WorldContentService contentService,
                             @NotNull ClientManager clientManager, @NotNull TravelHistory travelHistory,
                             @NotNull WorldHandler worldHandler, @NotNull IndicatorService indicators,
                             @NotNull SteeringService steering, @NotNull HazardService hazards,
                             @NotNull SightingService sightings, @NotNull WindService wind,
                             @NotNull ContestedLandingPolicy contests, @NotNull VoyageService voyageService) {
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
        this.steering = steering;
        this.hazards = hazards;
        this.sightings = sightings;
        this.wind = wind;
        this.contests = contests;
        this.voyageService = voyageService;
    }

    /** Whether expeditions are possible at all — false when the limbo template is missing from config. */
    public boolean canDepart() {
        return templates.get(limboTemplate).isPresent();
    }

    /** The expedition {@code player} is on, if any. */
    public @NotNull Optional<Expedition> expeditionOf(@NotNull UUID player) {
        return expeditions.values().stream().filter(expedition -> expedition.getCrew().has(player)).findFirst();
    }

    /**
     * Puts {@code crew} to sea. The roster freezes here: from this point nobody joins, nobody leaves, and stepping off
     * the deck means nothing but a swim back to it.
     */
    public @NotNull CompletableFuture<Boolean> begin(@NotNull Crew crew) {
        // Opening an expedition waits on a world being cloned, so a second click lands before the first has anything to
        // show for itself. Without this the captain gets two oceans and their crew is split between them.
        if (!opening.add(crew.getCaptain())) {
            return CompletableFuture.completedFuture(false);
        }

        final Optional<IslandTemplate> template = templates.get(limboTemplate);
        if (template.isEmpty()) {
            log.warn("No island template '{}' - expeditions cannot run", limboTemplate).submit();
            opening.remove(crew.getCaptain());
            return CompletableFuture.completedFuture(false);
        }

        return instances.allocate(template.get()).thenApply(limbo -> {
            crewService.depart(crew);
            try {
                final Expedition expedition = setSail(crew, limbo);
                if (expedition == null) {
                    abandon(crew, limbo);
                    return false;
                }
                expeditions.put(crew.getCaptain(), expedition);
                return true;
            } catch (RuntimeException exception) {
                // Anything thrown past here would leave the crew flagged as sailing with no expedition to end it: the
                // helm refuses them, the hull will not re-crew them, and nothing else ever puts them back ashore.
                log.error("Could not put crew {} to sea", crew.getCaptain(), exception).submit();
                abandon(crew, limbo);
                return false;
            } finally {
                opening.remove(crew.getCaptain());
            }
        }).exceptionally(throwable -> {
            opening.remove(crew.getCaptain());
            log.error("Could not open an expedition for {}", crew.getCaptain(), throwable).submit();
            aboard(crew).forEach(sailor ->
                    UtilMessage.message(sailor, "clans.prefix.ship", "clans.discovery.failed"));
            return false;
        });
    }

    /**
     * Moves the crew onto their ship, out in open water, and starts the simulation under it.
     * <p>
     * The limbo template declares a mooring but no vessel, so the hull put there is the one they boarded at the dock —
     * one template serves the whole fleet.
     *
     * @return the running expedition, or null if they could not be put to sea
     */
    private Expedition setSail(@NotNull Crew crew, @NotNull IslandInstance limbo) {
        final World world = Bukkit.getWorld(limbo.getWorldName());
        if (world == null) {
            log.warn("Limbo world '{}' vanished before the crew could board", limbo.getWorldName()).submit();
            return null;
        }

        final Berth moored = moorTheirVessel(world, crew).orElse(null);
        if (moored == null || moored.getHull() == null) {
            log.warn("Crew {} has no hull to sail in '{}'", crew.getCaptain(), world.getName()).submit();
            return null;
        }

        final Location anchor = moored.getAnchor();
        final ShipDynamicsConfig handling = new ShipDynamicsConfig(baseSpeed, rudderInRate, rudderOutRate,
                rudderResistance, maxYawRate, hullResponse, turnDrag);
        final Expedition expedition = new Expedition(crew, limbo,
                new ShipDynamics(handling, anchor.getYaw()), new Ocean(0, 0),
                moored.getHull(), anchor, waterLevelAt(anchor), System.currentTimeMillis());

        final List<Player> sailors = aboard(crew);

        // Before the teleports, not after them. The reaper releases any instance that reports itself empty, and a
        // warm-pool ocean is already past its grace period the moment it is claimed - so an expedition that waits for
        // its boarding teleport to land can have the world destroyed out from under it in the gap.
        sailors.forEach(sailor -> instances.enter(limbo, sailor));

        for (Player sailor : sailors) {
            relocate(sailor, moored.getBoard());
            castOffCues(sailor);
        }
        return expedition;
    }

    /**
     * Where the ship floats, scanned down from the paste anchor. Found once because the hull never moves, and probing
     * blocks on every tick for a number that cannot change is work for nothing.
     */
    private double waterLevelAt(@NotNull Location anchor) {
        final World world = anchor.getWorld();
        if (world != null) {
            for (int y = anchor.getBlockY(); y > world.getMinHeight(); y--) {
                if (world.getBlockAt(anchor.getBlockX(), y, anchor.getBlockZ()).getType() == Material.WATER) {
                    return y + 1.0;
                }
            }
        }
        log.warn("No water under the mooring at {} - the sea will be drawn at the berth's own height", anchor).submit();
        return anchor.getY();
    }

    /** Calls an expedition off before it started, releasing the ocean and letting the crew carry on where they are. */
    private void abandon(@NotNull Crew crew, @NotNull IslandInstance limbo) {
        aboard(crew).forEach(sailor ->
                UtilMessage.message(sailor, "clans.prefix.ship", "clans.discovery.failed"));

        crewService.disband(crew);
        shipService.clearAssignments(limbo.getWorldName());
        instances.release(limbo.getId(), true);
    }

    /**
     * Puts the crew's own ship in their patch of ocean.
     * <p>
     * The limbo template names no vessel — its berth is an empty mooring — so the hull is whichever one they were
     * standing on when they set out. Reloading the world's content rather than only pasting blocks is what brings the
     * helm and the quartermaster with it: they are ordinary data-points inside the structure, and something has to
     * install them.
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
            return Optional.empty();
        }

        shipService.assign(limbo, mooring.getId(), vessel);
        contentService.loadWorld(limbo);

        return shipService.berth(limbo, mooring.getId());
    }

    /** The structure the crew boarded at the dock, read back off the berth they mustered at. */
    private @NotNull String vesselOf(@NotNull Crew crew) {
        return originBerth(crew).map(Berth::getStructure).orElse("");
    }

    /**
     * Steps every running simulation.
     * <p>
     * The step is worth the time that actually passed rather than the interval that was asked for: a lagging server
     * delivers this late, and a fixed step would quietly make the ship slower for everyone aboard it.
     */
    @UpdateEvent(delay = 50)
    public void tick() {
        final long now = System.currentTimeMillis();
        final double dt = Math.clamp((now - lastTickAt) / 1000.0, 0.0, 1.0);
        lastTickAt = now;
        if (dt <= 0 || expeditions.isEmpty()) {
            return;
        }

        for (Expedition expedition : expeditions.values()) {
            final ShipDynamics dynamics = expedition.getDynamics();
            final List<Player> crew = stillAtSea(expedition);
            final int crewInput = steering.tick(expedition, crew, now);

            // Before the step, so the weather the crew was just told about is the weather this step is sailed in.
            wind.tick(expedition, crew, dt, now);

            // A hazard with hold of the wheel outranks whatever the crew is doing with the controls.
            dynamics.advance(dt, hazards.steer(expedition, crewInput, now));
            expedition.getOcean().advance(dt, dynamics.getSpeed(), dynamics.getHeading());

            // Last, so both drift against the position the ship has just reached rather than the one it left.
            hazards.tick(expedition, dt, now);

            // An iceberg ends the expedition inside that call and hands its subsystems back. Ticking one afterwards
            // would rebuild it for a crew already ashore, leaking the field and any markers it then spawns.
            if (!isRunning(expedition)) {
                continue;
            }

            sightings.tick(expedition, dt, now);
        }
    }

    /**
     * Whether this expedition is still the live one for its captain. A crossing can end part-way through its own tick,
     * and identity rather than presence is the test: the captain may already have put to sea again.
     */
    private boolean isRunning(@NotNull Expedition expedition) {
        return expeditions.get(expedition.getCrew().getCaptain()) == expedition;
    }

    /**
     * A speed and heading readout on the action bar, at the lowest priority so anything with something to say sits
     * above it. Provisional: it exists so the simulation is visibly running before there is any sea to look at.
     */
    @UpdateEvent(delay = 250)
    public void reportProgress() {
        for (Expedition expedition : expeditions.values()) {
            final Component speed = Component.text(String.format("%.1f", expedition.getDynamics().getSpeed()),
                    NamedTextColor.WHITE);
            final Component heading = Component.text(String.format("%03.0f", expedition.getDynamics().getHeading()),
                    NamedTextColor.AQUA);

            for (Player sailor : stillAtSea(expedition)) {
                gamer(sailor).ifPresent(gamer -> gamer.getActionBar().add(EXPEDITION_ACTION_BAR_PRIORITY,
                        new TimedComponent(0.5, false,
                                gmr -> Translations.component("clans.discovery.actionbar", speed, heading))));
            }
        }
    }

    /**
     * Ends a crossing that the sea ended for them. A wreck earns a title rather than a line of chat, and it has to be
     * raised before the recall, while the crew is still aboard to be told.
     */
    public void strand(@NotNull Expedition expedition) {
        for (Player sailor : stillAtSea(expedition)) {
            // Above the rudder gauge, which is still being pushed to whoever had hold of a control.
            gamer(sailor).ifPresent(gamer -> gamer.getTitleQueue().add(100, new TitleComponent(0.2, 2.4, 0.6, false,
                    viewer -> Translations.component("clans.discovery.stranded-title").color(NamedTextColor.RED),
                    viewer -> Translations.component("clans.discovery.stranded").color(NamedTextColor.GRAY))));
        }

        recall(expedition, null);
    }

    /**
     * Ends an expedition and puts the crew back on the dock they left from.
     * <p>
     * The reason is a key rather than a message because coming about is only one way this ends: the same return runs
     * for a crew that has been stranded, and the difference between those is one line of text.
     */
    public void recall(@NotNull Expedition expedition, @Nullable String reasonKey) {
        final Crew crew = expedition.getCrew();
        if (!expeditions.remove(crew.getCaptain(), expedition)) {
            return; // already ended; a second return would teleport people out of wherever they went next
        }

        releaseSubsystems(expedition);

        final Location dock = originBerth(crew).map(Berth::getBoard).orElse(null);

        final List<CompletableFuture<Boolean>> ashore = new ArrayList<>();
        for (Player sailor : stillAtSea(expedition)) {
            if (reasonKey != null) {
                UtilMessage.message(sailor, "clans.prefix.ship", reasonKey);
            }
            ashore.add(relocate(sailor, dock != null ? dock : fallbackFor(sailor)));
            landfallCues(sailor);
        }

        crewService.disband(crew);

        // Empty the mooring before the world goes, so a recycled instance never starts with the last crew's ship.
        shipService.clearAssignments(expedition.getLimbo().getWorldName());

        // Only once everybody is off it. Destroying an instance clears whoever is still inside to the server's fallback
        // world first, and that would beat a return teleport still in flight - dropping the crew at spawn instead of at
        // the dock they set out from.
        CompletableFuture.allOf(ashore.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, error) -> instances.release(expedition.getLimbo().getId(), true));
    }

    /**
     * Ends an expedition by putting the whole crew ashore on the island they sighted and steered for.
     * <p>
     * They land together or not at all: the first ashore claims the instance and the rest follow into it, which only
     * works while they are still a crew — so the roster is not disbanded until the last of them has arrived.
     * <p>
     * An island a comparable crew reached moments ago is not handed out a second time. That crew is still on it, and
     * this one is put down beside them to settle who keeps it.
     */
    public void makeLandfall(@NotNull Expedition expedition, @NotNull IslandOffer offer) {
        final Crew crew = expedition.getCrew();
        if (!expeditions.remove(crew.getCaptain(), expedition)) {
            return; // already ended; a second landing would teleport people out of wherever they went next
        }

        releaseSubsystems(expedition);

        final List<Player> landing = stillAtSea(expedition);
        final long now = System.currentTimeMillis();

        final ContestedLanding contested = contests.contestFor(offer.getTemplate().getKey(), landing.size(), now)
                .orElse(null);
        if (contested != null && landIntoContest(expedition, landing, contested)) {
            return;
        }

        for (Player sailor : landing) {
            UtilMessage.message(sailor, "clans.prefix.ship", "clans.discovery.sighting.landfall", offer.displayName());
            landfallCues(sailor);
        }

        final CompletableFuture<Boolean> first = landing.isEmpty()
                ? CompletableFuture.completedFuture(true)
                : putAshore(landing.getFirst(), offer);

        first.thenCompose(ignored -> {
            // The instance only exists once the first sailor has been given it, and it is the first sailor's arrival
            // that says which one it is - so the landing is on the books from here rather than from the top.
            recordLanding(offer, landing, now);

            // Only now: an island the first sailor has not been given yet is not one the rest can be let into, and each
            // of them would allocate a copy of their own.
            final List<CompletableFuture<Boolean>> rest = landing.stream().skip(1)
                    .map(sailor -> putAshore(sailor, offer))
                    .toList();
            return CompletableFuture.allOf(rest.toArray(CompletableFuture[]::new));
        }).whenComplete((ignored, error) -> {
            if (error != null) {
                log.warn("Landfall for crew {} did not complete cleanly", crew.getCaptain(), error).submit();
            }

            crewService.disband(crew);

            // Empty the mooring before the world goes, so a recycled instance never starts with the last crew's ship.
            shipService.clearAssignments(expedition.getLimbo().getWorldName());

            // Only once everybody is off it. Destroying an instance clears whoever is still inside to the server's
            // fallback world first, and that would beat an arrival still in flight.
            instances.release(expedition.getLimbo().getId(), true);
        });
    }

    /**
     * Puts a whole crew down on an island another crew already holds.
     * <p>
     * The instance is entered directly rather than through the offer: the offer allocates, and allocating here is
     * exactly what must not happen — the point is that both crews are on the same ground.
     *
     * @return whether they were delivered, false if the island has gone since it was recorded
     */
    private boolean landIntoContest(@NotNull Expedition expedition, @NotNull List<Player> landing,
                                    @NotNull ContestedLanding contested) {
        final IslandInstance instance = instances.find(contested.getInstanceId()).orElse(null);
        if (instance == null) {
            return false;
        }

        final World island = Bukkit.getWorld(instance.getWorldName());
        if (island == null) {
            return false;
        }

        final Location shore = contestedShore(island, contested.getArrivalPointName());

        for (UUID holder : instance.getOccupants()) {
            final Player defender = Bukkit.getPlayer(holder);
            if (defender != null) {
                UtilMessage.message(defender, "clans.prefix.ship", "clans.discovery.contest.sails-sighted");
                new SoundEffect(Sound.EVENT_RAID_HORN, 0.5f, 1.2f).play(defender);
            }
        }

        // Before the teleports, not after them. The reaper releases any instance that reports itself empty, and a
        // landing crew that waits for its arrival to complete could have the island destroyed out from under it.
        landing.forEach(sailor -> instances.enter(instance, sailor));

        CompletableFuture<Boolean> arrivals = CompletableFuture.completedFuture(true);
        for (Player sailor : landing) {
            UtilMessage.message(sailor, "clans.prefix.ship", "clans.discovery.contest.not-alone");
            landfallCues(sailor);
            // One after another rather than all at once: a crew arriving in a burst of cross-world teleports is a crew
            // half of whom are still in flight when the ocean under them is handed back.
            arrivals = arrivals.thenCompose(ignored -> relocate(sailor, shore));
        }

        arrivals.whenComplete((ignored, error) -> {
            if (error != null) {
                log.warn("Contested landing for crew {} did not complete cleanly",
                        expedition.getCrew().getCaptain(), error).submit();
            }

            crewService.disband(expedition.getCrew());
            shipService.clearAssignments(expedition.getLimbo().getWorldName());
            instances.release(expedition.getLimbo().getId(), true);
        });
        return true;
    }

    /**
     * Where a contesting crew comes ashore: a landing the crew already there did not use, the same one if the island has
     * only the one, and the world's own spawn if it has none at all.
     */
    private @NotNull Location contestedShore(@NotNull World island, @NotNull String taken) {
        final List<ArrivalPoint> points = voyageService.arrivals(island);
        if (points.isEmpty()) {
            return island.getSpawnLocation();
        }

        final List<ArrivalPoint> elsewhere = points.stream()
                .filter(point -> !point.getName().equalsIgnoreCase(taken))
                .toList();
        final List<ArrivalPoint> candidates = elsewhere.isEmpty() ? points : elsewhere;

        final List<String> names = candidates.stream().map(ArrivalPoint::getName).toList();
        final int chosen = Math.floorMod(ArrivalDistribution.random().select(names), candidates.size());
        return candidates.get(chosen).getLocation();
    }

    /** Puts a fresh landing on the books, so a comparable crew arriving behind them sails into a fight instead. */
    private void recordLanding(@NotNull IslandOffer offer, @NotNull List<Player> landing, long now) {
        if (landing.isEmpty()) {
            return;
        }

        final Player first = landing.getFirst();
        instances.byOccupant(first.getUniqueId()).ifPresent(instance -> contests.record(offer.getTemplate().getKey(),
                landing.size(), instance.getId(), arrivalNameAt(instance, first.getLocation()), now));
    }

    /**
     * The name of the landing a crew came ashore at.
     * <p>
     * Read back from where they ended up rather than chosen up front, because the offer delivers them itself and only
     * reports that they arrived. An island with no markers at all names nothing, which leaves every shore on it open to
     * a crew arriving behind them.
     */
    private @NotNull String arrivalNameAt(@NotNull IslandInstance instance, @NotNull Location ashore) {
        final World island = Bukkit.getWorld(instance.getWorldName());
        if (island == null || !island.equals(ashore.getWorld())) {
            return "";
        }

        return voyageService.arrivals(island).stream()
                .min(Comparator.comparingDouble(point -> point.getLocation().distanceSquared(ashore)))
                .map(ArrivalPoint::getName)
                .orElse("");
    }

    /**
     * Puts one sailor on the island.
     * <p>
     * The offer teleports them itself, across worlds, and Paper refuses to move anybody carrying passengers — which a
     * captain does without ever being told, because their crew marker is mounted on them. Taking it down has to happen
     * here rather than inside the offer, or the captain silently stays at sea while their crew lands without them.
     */
    private @NotNull CompletableFuture<Boolean> putAshore(@NotNull Player sailor, @NotNull IslandOffer offer) {
        indicators.detach(sailor);
        sailor.eject();

        relocating.add(sailor.getUniqueId());
        return offer.receive(sailor).whenComplete((arrived, error) -> {
            relocating.remove(sailor.getUniqueId());
            if (error != null) {
                log.warn("Could not put {} ashore on {}", sailor.getName(), offer.getTemplate().getKey(), error).submit();
            } else if (!Boolean.TRUE.equals(arrived)) {
                log.warn("Landing of {} on {} was refused", sailor.getName(), offer.getTemplate().getKey()).submit();
            }
        });
    }

    /**
     * Hands back everything attached to a running expedition. The controls are eager scene objects and the markers are
     * loose displays; either left behind would stay registered against a world that no longer exists.
     */
    private void releaseSubsystems(@NotNull Expedition expedition) {
        steering.release(expedition);
        hazards.release(expedition);
        sightings.release(expedition);
        wind.release(expedition);
    }

    /** The mooring the crew formed on, back in the world they set out from. */
    private @NotNull Optional<Berth> originBerth(@NotNull Crew crew) {
        final World origin = Bukkit.getWorld(crew.getWorldName());
        if (origin == null) {
            return Optional.empty();
        }
        return shipService.berth(origin, crew.getBerthId());
    }

    /** Where a sailor goes when their dock cannot be found: back where they set out from, else spawn. */
    private @NotNull Location fallbackFor(@NotNull Player sailor) {
        return travelHistory.origin(sailor)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation);
    }

    /**
     * Puts anyone who goes over the side back on deck.
     * <p>
     * At the dock, stepping off the hull is how you leave a crew. Out here it cannot mean that — there is nothing
     * around but open water — so a mistimed jump would leave somebody treading water until the expedition ended.
     */
    @EventHandler
    public void onOverboard(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final Expedition expedition = expeditionOf(event.getPlayer().getUniqueId()).orElse(null);
        if (expedition == null || expedition.isAboard(event.getTo())) {
            return;
        }

        final World limbo = Bukkit.getWorld(expedition.getLimbo().getWorldName());
        if (limbo == null || !limbo.equals(event.getTo().getWorld())) {
            return;
        }

        final Berth deck = shipService.berths(limbo).stream().findFirst().orElse(null);
        if (deck == null || !deck.isCrewable()) {
            return;
        }

        relocate(event.getPlayer(), deck.getBoard());
        UtilMessage.message(event.getPlayer(), "clans.prefix.ship", "clans.discovery.hauled-aboard");
        new SoundEffect(Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f).play(event.getPlayer());
    }

    /**
     * Ends a sailor's expedition when something moves them off the ocean — a home command, an admin, anything.
     * <p>
     * This service's own teleports are excluded: reading those as somebody leaving would cancel every expedition at the
     * moment it succeeded.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportOut(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (relocating.contains(player.getUniqueId()) || event.getTo() == null) {
            return;
        }

        final Expedition expedition = expeditionOf(player.getUniqueId()).orElse(null);
        if (expedition == null) {
            return;
        }

        final String limboWorld = expedition.getLimbo().getWorldName();
        if (!limboWorld.equals(event.getFrom().getWorld().getName())
                || limboWorld.equals(event.getTo().getWorld().getName())) {
            return; // never aboard, or still aboard and only moved
        }

        // Leaving the ocean ends it for them alone; the rest of the crew sails on.
        crewService.leave(player.getUniqueId());
        UtilMessage.message(player, "clans.prefix.ship", "clans.discovery.left");
    }

    /**
     * Hands the ocean back when the last person aboard logs out.
     * <p>
     * An expedition whose crew is entirely offline has nobody left to come about, and its cloned world would otherwise
     * sit allocated until the server restarted.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final Expedition expedition = expeditionOf(event.getPlayer().getUniqueId()).orElse(null);
        if (expedition == null) {
            return;
        }

        final boolean anyoneLeft = aboard(expedition.getCrew()).stream()
                .anyMatch(sailor -> !sailor.getUniqueId().equals(event.getPlayer().getUniqueId()));
        if (!anyoneLeft) {
            recall(expedition, "clans.discovery.came-about");
        }
    }

    private void castOffCues(@NotNull Player sailor) {
        effects.addEffect(sailor, EffectTypes.BLINDNESS, 1, BLINDNESS_MILLIS);
        title(sailor, Translations.component("clans.discovery.title.departing"),
                Translations.component("clans.discovery.title.open-sea").color(NamedTextColor.GRAY));

        new SoundEffect(Sound.ENTITY_BOAT_PADDLE_WATER, 0.7f, 1.0f).play(sailor);
        new SoundEffect(Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 0.6f, 0.8f).play(sailor);
        new SoundEffect(Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.5f).play(sailor);
    }

    private void landfallCues(@NotNull Player sailor) {
        effects.addEffect(sailor, EffectTypes.BLINDNESS, 1, BLINDNESS_MILLIS);
        title(sailor, Translations.component("clans.discovery.title.in-port").color(NamedTextColor.GOLD),
                Translations.component("clans.discovery.title.moored").color(NamedTextColor.GRAY));

        new SoundEffect(Sound.ENTITY_BOAT_PADDLE_LAND, 0.8f, 1.0f).play(sailor);
        new SoundEffect(Sound.AMBIENT_UNDERWATER_EXIT, 1.0f, 0.6f).play(sailor);
    }

    /**
     * Moves a sailor, marking the teleport as ours so {@link #onTeleportOut} does not read it as them walking out.
     * <p>
     * A refused move is reported rather than swallowed. Every visible symptom of an expedition going wrong is somebody
     * standing where they were, and without this the one event that says so is discarded.
     */
    private @NotNull CompletableFuture<Boolean> relocate(@NotNull Player sailor, @NotNull Location destination) {
        // Paper refuses to move anybody carrying passengers to another world, and a captain carries one without ever
        // being told: their crew marker is mounted on them. Every leg changes world, so the marker has to come down
        // first or the teleport simply reports false and the ship never leaves.
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

    private void title(@NotNull Player sailor, @NotNull Component heading, @NotNull Component sub) {
        gamer(sailor).ifPresent(gamer -> gamer.getTitleQueue().add(EXPEDITION_TITLE_PRIORITY,
                new TitleComponent(0.2, 1.6, 0.4, false, gmr -> heading, gmr -> sub)));
    }

    private @NotNull Optional<Gamer> gamer(@NotNull Player player) {
        return Optional.ofNullable(clientManager.search().online(player)).map(client -> client.getGamer());
    }

    /**
     * The sailors still on the expedition's own stretch of ocean.
     * <p>
     * Anyone who left — by command, by an admin, by anything that moved them off the ship — has already ended their
     * expedition, and hauling them back to the dock later would teleport them out of whatever they went off to do.
     */
    private @NotNull List<Player> stillAtSea(@NotNull Expedition expedition) {
        final String limboWorld = expedition.getLimbo().getWorldName();
        return aboard(expedition.getCrew()).stream()
                .filter(sailor -> sailor.getWorld().getName().equals(limboWorld))
                .toList();
    }

    /** The crew members currently online. Offline sailors simply miss the trip rather than holding it up. */
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
