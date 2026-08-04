package me.mykindos.betterpvp.clans.world.ship;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.crew.menu.CrewMenu;
import me.mykindos.betterpvp.clans.world.island.IslandOfferProvider;
import me.mykindos.betterpvp.clans.world.navigation.NavigationMenu;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.travel.TravelService;
import me.mykindos.betterpvp.clans.world.voyage.VoyageDestination;
import me.mykindos.betterpvp.clans.world.voyage.VoyageDestinationRegistry;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.indicator.Indicator;
import me.mykindos.betterpvp.core.scene.indicator.IndicatorService;
import me.mykindos.betterpvp.core.scene.indicator.ModelIndicatorStyle;
import me.mykindos.betterpvp.core.scene.interaction.SceneInteractionRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Ties the ship's fittings to the crew system: boarding, the quartermaster's book, and the markers that tell people
 * where to look.
 * <p>
 * The indicators are reconciled on a beat rather than driven by callbacks out of {@code CrewService}. A crew can end in
 * several ways — walking off, logging out, being disbanded, sailing — and threading a hook through every one of them is
 * more places to forget than a periodic check that simply makes the markers match the crews.
 */
@BPvPListener
@Singleton
public class ShipInteractions implements Listener {

    /** Above a captain's head, and over the quartermaster; the same marker in both places. */
    private static final String INDICATOR_MODEL = "arrow_indicator";
    private static final String INDICATOR_IDLE = "bounce";

    private static final double CAPTAIN_ICON_HEIGHT = 0.8;
    private static final double ARROW_HEIGHT = 2.4;

    private final CrewService crewService;
    private final ShipService shipService;
    private final IndicatorService indicators;

    private final Map<UUID, Indicator> captainIcons = new HashMap<>();
    private final Map<String, Indicator> requestArrows = new HashMap<>();

    private final VoyageDestinationRegistry voyageDestinations;
    private final IslandOfferProvider islandOffers;
    private final TravelService travelService;
    private final ClientManager clientManager;

    @Inject
    public ShipInteractions(@NotNull CrewService crewService, @NotNull ShipService shipService,
                            @NotNull IndicatorService indicators, @NotNull SceneInteractionRegistry sceneInteractions,
                            @NotNull VoyageDestinationRegistry voyageDestinations,
                            @NotNull IslandOfferProvider islandOffers,
                            @NotNull TravelService travelService, @NotNull ClientManager clientManager) {
        this.crewService = crewService;
        this.shipService = shipService;
        this.indicators = indicators;
        this.voyageDestinations = voyageDestinations;
        this.islandOffers = islandOffers;
        this.travelService = travelService;
        this.clientManager = clientManager;

        sceneInteractions.register(ShipService.CREW_INTERACTION, (player, placement) -> openCrewBook(player));
    }

    /**
     * The wheel. Only the captain sets a course — everyone else aboard is told whose call it is rather than being
     * shown a menu that would refuse them.
     * <p>
     * Called straight from the {@code ship_helm} data-point rather than through an interaction name, because a wheel is
     * only ever a wheel: see {@link ShipHelms}.
     */
    public void takeTheWheel(@NotNull Player player) {
        final Optional<Crew> crew = crewService.captainedBy(player.getUniqueId());
        if (crew.isEmpty()) {
            crewService.crewOf(player.getUniqueId()).ifPresentOrElse(
                    sailing -> UtilMessage.message(player, "clans.prefix.ship", "clans.ship.not-captain"),
                    () -> UtilMessage.message(player, "clans.prefix.ship", "clans.ship.no-ship"));
            return;
        }

        if (crew.get().isSailing()) {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.already-sailing");
            return;
        }

        // Sailing to where you already are is not a journey. Filtered out rather than refused on click, so the menu
        // never offers a course that cannot be set.
        final String here = player.getWorld().getName();
        final List<Destination> destinations = new ArrayList<>(voyageDestinations.available().stream()
                .filter(destination -> !(destination instanceof VoyageDestination voyage)
                        || !voyage.getWorldName().equals(here))
                .toList());

        // Resource islands sit in the same list as the fixed ports: a crossing to one is the same crossing, and the
        // only thing that marks it out is that the place is made when the crew gets there rather than waiting for them.
        islandOffers.destinationsFor(player).stream()
                .filter(Destination::isReady)
                .forEach(destinations::add);

        if (destinations.isEmpty()) {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.nowhere-to-sail");
            return;
        }

        // Routed through TravelService so the crossing inherits the travel guards - no leaving mid-fight, no double
        // departure - but with no hold: the crossing itself is the wait.
        new NavigationMenu(destinations, travelService, true).show(player);
        new SoundEffect(Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1.2f, 0.7f).play(player);
    }

    /**
     * Puts {@code player} aboard as a captain, facing the way the berth faces — which is how they end up looking at the
     * helm without anything having to aim them at it.
     */
    public void board(@NotNull Player player, @NotNull Berth berth) {
        if (!berth.isCrewable()) {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.not-ready");
            return;
        }

        // A captain already at sea cannot start a second crossing; they have to finish or leave the one they are on.
        if (crewService.crewOf(player.getUniqueId()).filter(Crew::isSailing).isPresent()) {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.already-sailing");
            return;
        }

        final Crew crew = crewService.muster(player.getUniqueId(), berth.getId(), berth.getWorldName(), berth.getCapacity());
        player.teleportAsync(berth.getBoard()).thenRun(() -> {
            UtilMessage.message(player, "clans.prefix.ship", "clans.ship.boarded");
            new SoundEffect(Sound.ENTITY_BOAT_PADDLE_WATER, 0.9f, 0.8f).play(player);
        });

        showCaptainIcon(crew);
        showRequestsArrow(berth);
    }

    /** The quartermaster's book — your crew if you have one, otherwise a nudge toward the navigator. */
    private void openCrewBook(@NotNull Player player) {
        final Optional<Crew> crew = crewService.crewOf(player.getUniqueId());
        if (crew.isEmpty()) {
            UtilMessage.message(player, "clans.prefix.crew", "clans.crew.not-crewed");
            return;
        }

        new CrewMenu(crewService, clientManager, crew.get(), player).show(player);
    }

    private void showCaptainIcon(@NotNull Crew crew) {
        captainIcons.computeIfAbsent(crew.getCaptain(), captainId -> {
            final Player captain = Bukkit.getPlayer(captainId);
            if (captain == null) {
                return null;
            }

            final Indicator indicator = indicators.attach(captain,
                    new ModelIndicatorStyle(INDICATOR_MODEL, INDICATOR_IDLE, 1.0, Display.Billboard.FIXED), 1.3);
            // Shown only to somebody who could actually click it: aboard the same hull, not a captain themselves, and
            // not already crewed. To anyone else it is decoration inviting a click that would do nothing.
            indicator.visibleTo(viewer -> canAsk(viewer, crew));
            return indicator;
        });
    }

    private boolean canAsk(@NotNull Player viewer, @NotNull Crew crew) {
        if (crew.isSailing() || crew.has(viewer.getUniqueId()) || crewService.isCaptain(viewer.getUniqueId())) {
            return false;
        }
        return shipService.berthAt(viewer.getLocation())
                .filter(berth -> berth.getId().equals(crew.getBerthId()))
                .isPresent();
    }

    /**
     * Hangs a bobbing arrow over the quartermaster. The bob is the model's own idle clip, so the marker sits at a fixed
     * spot rather than being moved about to fake motion.
     */
    private void showRequestsArrow(@NotNull Berth berth) {
        final Location stand = berth.getQuartermaster();
        if (stand == null) {
            return;
        }

        requestArrows.computeIfAbsent(BerthKey.of(berth), key -> {
            final Indicator indicator = indicators.attachAt(stand.clone().add(0, ARROW_HEIGHT, 0),
                    new ModelIndicatorStyle(INDICATOR_MODEL, INDICATOR_IDLE, 1.0, Display.Billboard.FIXED));
            indicator.visibleTo(viewer -> hasWaitingRequests(viewer, berth));
            return indicator;
        });
    }

    private boolean hasWaitingRequests(@NotNull Player viewer, @NotNull Berth berth) {
        return crewService.captainedBy(viewer.getUniqueId())
                .filter(crew -> crew.getBerthId().equals(berth.getId()))
                .filter(crew -> !crew.pendingRequests().isEmpty())
                .isPresent();
    }

    /**
     * Makes the markers match the crews. Captain icons for people who are no longer captains come down, and an arrow
     * whose berth has nobody crewing goes with them.
     */
    @UpdateEvent(delay = 1000)
    public void reconcileIndicators() {
        captainIcons.entrySet().removeIf(entry -> {
            if (crewService.isCaptain(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });

        // An arrow outlives a single crew — the next captain to board this hull wants it there — but not a berth that
        // nobody is crewing at all.
        requestArrows.entrySet().removeIf(entry -> {
            if (crewsAt(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });
    }

    /** Whether anyone is still gathering a crew at the berth this key names. */
    private boolean crewsAt(@NotNull String berthKey) {
        return !crewService.mustering(BerthKey.worldOf(berthKey), BerthKey.berthOf(berthKey)).isEmpty();
    }
}
