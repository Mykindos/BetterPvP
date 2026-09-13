package me.mykindos.betterpvp.clans.world.sailing;

import lombok.Getter;
import me.mykindos.betterpvp.core.menu.navigation.Destination;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.Party;
import me.mykindos.betterpvp.core.world.site.Placement;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.TransitTiming;
import me.mykindos.betterpvp.core.world.site.crew.Crew;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * One {@link Site} offered at a ship's helm as somewhere to travel to.
 * <p>
 * One class covers every kind of site, because which kind it is belongs to the site's own policy and nothing here has
 * to ask. Clicking it does not teleport the clicker. It starts a {@link Voyage} for their whole {@link Crew}, and the
 * instance is only located once that voyage ends, which for an instanced site is what avoids holding an empty world
 * open for the length of the journey.
 */
@Getter
public class ShipDestination implements Destination, Landfall {

    private final SiteKey siteKey;
    private final Site site;
    private final Component displayName;

    private final CrewService crewService;
    private final VoyageService voyageService;
    private final Placement placement;

    public ShipDestination(@NotNull Site site, @NotNull SiteKey siteKey, @NotNull Component displayName,
                           @NotNull CrewService crewService, @NotNull VoyageService voyageService,
                           @NotNull Placement placement) {
        this.site = site;
        this.siteKey = siteKey;
        this.displayName = displayName;
        this.crewService = crewService;
        this.voyageService = voyageService;
        this.placement = placement;
    }

    @Override
    public @NotNull Component displayName() {
        return displayName;
    }

    /** Built when the menu asks rather than when the course is offered, since most offers are never rendered. */
    @Override
    public @NotNull ItemView icon() {
        return ItemView.builder().material(site.getIcon()).displayName(displayName).build();
    }

    @Override
    public @NotNull TransitTiming timing() {
        return site.getTiming();
    }

    /**
     * Whether a course can be set at all. Whether the place itself is ready is not asked here: a site that has to be
     * made is made when the crew arrives, which is minutes from now.
     */
    public boolean isReady() {
        return placement.isLocal(site);
    }

    /**
     * Starts the voyage. Only a captain reaches here, since the helm refuses anybody else before the menu opens, and
     * it is their whole crew that leaves rather than they alone.
     */
    @Override
    public void select(@NotNull Player traveller) {
        final Optional<Crew> crew = crewService.captainedBy(traveller.getUniqueId());
        if (crew.isEmpty()) {
            UtilMessage.message(traveller, "clans.prefix.ship", "clans.ship.not-captain");
            return;
        }

        voyageService.begin(crew.get(), this);
    }

    /**
     * Finds the crew one instance and puts them all in it. The whole crew is asked for at once, which is what makes a
     * group that sailed together land together rather than on a copy each.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> setAshore(@NotNull List<Player> sailors) {
        if (sailors.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return placement.locate(siteKey, partyOf(sailors)).thenCompose(handle -> placement.sendAll(sailors, handle));
    }

    /**
     * The crew's own party less anybody who left on the way, since only those still at sea are being put ashore.
     * Order is kept so the party lands in the order it sailed, captain first.
     */
    private @NotNull Party partyOf(@NotNull List<Player> sailors) {
        final Set<UUID> members = new LinkedHashSet<>();
        sailors.forEach(sailor -> members.add(sailor.getUniqueId()));
        return Party.of(sailors.getFirst().getUniqueId(), members);
    }
}
