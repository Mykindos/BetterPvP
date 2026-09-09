package me.mykindos.betterpvp.clans.world.sailing;

import lombok.Getter;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.Party;
import me.mykindos.betterpvp.core.world.site.Placement;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import me.mykindos.betterpvp.core.world.travel.Destination;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A place a ship can sail to, offered at the helm.
 * <p>
 * One class covers every kind: a landmark that has always been there, one of several copies of a hub, and an island
 * that does not exist until the crew sights it. Which of those it is belongs to the site's own policy, and nothing
 * here has to ask. Clicking it does not teleport the clicker — it puts their whole crew to sea, and only when they
 * make landfall is an instance found for them.
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
    public @NotNull Key key() {
        return Key.key("betterpvp", "site/" + siteKey.getSiteId() + (siteKey.isOwned() ? "/" + siteKey.getOwnerId() : ""));
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
    public @NotNull VoyageTiming timing() {
        return site.getTiming();
    }

    /**
     * Whether a course can be set at all. Whether the place itself is ready is not asked here: a site that has to be
     * made is made when the crew arrives, which is minutes from now.
     */
    @Override
    public boolean isReady() {
        return placement.isLocal(site);
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

    private @NotNull Party partyOf(@NotNull List<Player> sailors) {
        final Set<UUID> members = new HashSet<>();
        sailors.forEach(sailor -> members.add(sailor.getUniqueId()));
        return Party.of(sailors.getFirst().getUniqueId(), members);
    }
}
