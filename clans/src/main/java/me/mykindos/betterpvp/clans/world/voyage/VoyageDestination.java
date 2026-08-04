package me.mykindos.betterpvp.clans.world.voyage;

import lombok.Getter;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A place a ship can sail to, offered at the helm.
 * <p>
 * Implemented as a {@link Destination} so the helm reuses the travel framework whole — the readiness check, the guards
 * that stop you leaving mid-fight, and the departure hold all apply without being restated. What differs is that
 * arriving does not teleport the clicker: it puts their whole crew to sea.
 * <p>
 * It is also its own {@link Landfall}: the world is already there, so the only thing left to decide when the crew
 * sights it is which of its docks they step onto.
 */
@Getter
public class VoyageDestination implements Destination, Landfall {

    private final Key key;
    private final Component displayName;
    private final ItemView icon;

    /** The world this leads to. Its {@code ship_arrive} markers are the candidate landings. */
    private final String worldName;

    private final VoyageTiming timing;
    private final ArrivalDistribution distribution;

    private final CrewService crewService;
    private final VoyageService voyageService;

    public VoyageDestination(@NotNull Key key, @NotNull Component displayName, @NotNull ItemView icon,
                             @NotNull String worldName, @NotNull VoyageTiming timing,
                             @NotNull ArrivalDistribution distribution,
                             @NotNull CrewService crewService, @NotNull VoyageService voyageService) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.worldName = worldName;
        this.timing = timing;
        this.distribution = distribution;
        this.crewService = crewService;
        this.voyageService = voyageService;
    }

    @Override
    public @NotNull Key key() {
        return key;
    }

    @Override
    public @NotNull Component displayName() {
        return displayName;
    }

    @Override
    public @NotNull ItemView icon() {
        return icon;
    }

    @Override
    public @NotNull VoyageTiming timing() {
        return timing;
    }

    /**
     * Sets the crew down at one of this world's docks, chosen once so they land together.
     * <p>
     * The world is checked again here rather than trusted from {@link #isReady()}: a crossing is minutes long, and a
     * world that was loaded when the course was set can be gone by the time the crew sights it.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> setAshore(@NotNull List<Player> sailors) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }

        final Optional<Location> landing = voyageService.landingIn(world, distribution);
        if (landing.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        final List<CompletableFuture<Boolean>> ashore = sailors.stream()
                .map(sailor -> voyageService.putAshore(sailor, landing.get()))
                .toList();
        return CompletableFuture.allOf(ashore.toArray(CompletableFuture[]::new)).thenApply(ignored -> true);
    }

    /**
     * Somewhere you cannot get to is not offered — and for a crossing that means somewhere you cannot be <em>put
     * ashore</em> as much as somewhere that is not loaded. A world with no landing is the worse of the two: the crew
     * spends the whole voyage at sea before anything can discover there is nowhere to arrive.
     */
    @Override
    public boolean isReady() {
        return Bukkit.getWorld(worldName) != null && voyageService.canDepart() && voyageService.canLand(worldName);
    }

    /** A course is set here, not walked; the crew is welcomed when they make landfall. */
    @Override
    public boolean announcesArrival() {
        return false;
    }

    /**
     * Sets the course. Only the captain reaches here — the helm refuses anyone else before the menu opens — and it is
     * their crew, not they alone, that leaves.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller) {
        final Optional<Crew> crew = crewService.captainedBy(traveller.getUniqueId());
        if (crew.isEmpty()) {
            UtilMessage.message(traveller, "clans.prefix.ship", "clans.ship.not-captain");
            return CompletableFuture.completedFuture(false);
        }

        return voyageService.begin(crew.get(), this);
    }
}
