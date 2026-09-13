package me.mykindos.betterpvp.clans.world.sailing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.menu.navigation.Destination;
import me.mykindos.betterpvp.core.menu.navigation.DestinationProvider;
import me.mykindos.betterpvp.core.world.site.Placement;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteOwners;
import me.mykindos.betterpvp.core.world.site.SitePolicy;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What a ship's helm offers: the sites that always exist, a few of the ones made on demand, and whatever the player
 * looking at it owns.
 * <p>
 * All of them come from the same catalogue and take the same journey. What marks an on-demand site out is that its
 * instance is made on arrival, so it is offered under a name drawn for the occasion rather than its own and only a
 * few are shown at a time. Those names are held per player for half a minute, so the menu does not reshuffle under
 * somebody who is still reading it.
 */
@Singleton
public class ShipDestinations implements DestinationProvider {

    private final SiteRegistry registry;
    private final SiteInstances instances;
    private final CrewService crewService;
    private final VoyageService voyageService;
    private final Placement placement;
    private final SiteOwners owners;
    private final Cache<UUID, List<Destination>> expeditionCache;

    @Inject
    @Config(path = "clans.voyage.expeditions", defaultValue = "3")
    private int expeditionCount;

    @Inject
    public ShipDestinations(@NotNull SiteRegistry registry, @NotNull SiteInstances instances,
                            @NotNull CrewService crewService, @NotNull VoyageService voyageService,
                            @NotNull Placement placement, @NotNull SiteOwners owners) {
        this.registry = registry;
        this.instances = instances;
        this.crewService = crewService;
        this.voyageService = voyageService;
        this.placement = placement;
        this.owners = owners;
        this.expeditionCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();
    }

    @Override
    public @NotNull List<Destination> destinationsFor(@NotNull Player player) {
        final Optional<String> here = siteOf(player);

        final List<Destination> destinations = new ArrayList<>();
        for (Site site : registry.all()) {
            if (here.filter(site.getId()::equals).isPresent()) {
                continue;
            }

            final Optional<ShipDestination> offer = isPort(site)
                    ? Optional.of(destination(site, site.key(), site.getDisplayName()))
                    : owned(site, player);

            offer.filter(ShipDestination::isReady).ifPresent(destinations::add);
        }

        destinations.addAll(expeditionCache.get(player.getUniqueId(), id -> expeditions()));
        return destinations;
    }

    /**
     * A site belonging to the player looking at the helm, offered only to them. Somebody who owns no instance of it
     * is offered nothing, which is how a player with no clan is shown no camp.
     */
    private @NotNull Optional<ShipDestination> owned(@NotNull Site site, @NotNull Player player) {
        if (site.getPolicy().getLifecycle() != SitePolicy.Lifecycle.OWNED) {
            return Optional.empty();
        }

        return owners.keyFor(site, player).map(key -> destination(site, key, site.getDisplayName()));
    }

    /** A site that exists whether or not anybody travels to it, and so is offered under its own name. */
    private boolean isPort(@NotNull Site site) {
        final SitePolicy.Lifecycle lifecycle = site.getPolicy().getLifecycle();
        return lifecycle == SitePolicy.Lifecycle.PERMANENT || lifecycle == SitePolicy.Lifecycle.POOLED;
    }

    private @NotNull List<Destination> expeditions() {
        final List<Site> candidates = new ArrayList<>();
        for (Site site : registry.all()) {
            if (site.getPolicy().getLifecycle() == SitePolicy.Lifecycle.ON_DEMAND) {
                candidates.add(site);
            }
        }
        Collections.shuffle(candidates);

        final List<Destination> offers = new ArrayList<>();
        for (Site site : candidates) {
            if (offers.size() >= expeditionCount) {
                break;
            }

            final ShipDestination offer = destination(site, site.key(), Component.text(PlaceNames.generate()));
            if (offer.isReady() && hasRoom(site)) {
                offers.add(offer);
            }
        }
        return offers;
    }

    /** Whether the site may still open another instance, so the helm never offers a course that would be refused. */
    private boolean hasRoom(@NotNull Site site) {
        final int max = site.getPolicy().getMax();
        return max <= 0 || instances.forKey(site.key()).size() < max;
    }

    private @NotNull ShipDestination destination(@NotNull Site site, @NotNull SiteKey key,
                                                 @NotNull Component displayName) {
        return new ShipDestination(site, key, displayName, crewService, voyageService, placement);
    }

    /** The site a player is standing on, if the world they are in belongs to one. */
    private @NotNull Optional<String> siteOf(@NotNull Player player) {
        return instances.byWorld(player.getWorld().getName()).map(instance -> instance.getKey().getSiteId());
    }
}
