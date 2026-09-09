package me.mykindos.betterpvp.clans.world.sailing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.world.site.Placement;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SitePolicy;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import me.mykindos.betterpvp.core.world.travel.Destination;
import me.mykindos.betterpvp.core.world.travel.DestinationProvider;
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
 * What a helm offers: the ports that are always there, plus a handful of uncharted islands.
 * <p>
 * Both come from the same catalogue and sail the same crossing. The only thing that marks an expedition out is that
 * it is made when the crew gets there, so it is offered under a name drawn for the occasion rather than its own, and
 * only a few are shown at a time. Those names are held per player for half a minute so the menu does not reshuffle
 * under somebody who is still reading it.
 */
@Singleton
public class ShipDestinations implements DestinationProvider {

    private final SiteRegistry registry;
    private final SiteInstances instances;
    private final CrewService crewService;
    private final VoyageService voyageService;
    private final Placement placement;
    private final Cache<UUID, List<Destination>> expeditionCache;

    @Inject
    @Config(path = "clans.voyage.expeditions", defaultValue = "3")
    private int expeditionCount;

    @Inject
    public ShipDestinations(@NotNull SiteRegistry registry, @NotNull SiteInstances instances,
                            @NotNull CrewService crewService, @NotNull VoyageService voyageService,
                            @NotNull Placement placement) {
        this.registry = registry;
        this.instances = instances;
        this.crewService = crewService;
        this.voyageService = voyageService;
        this.placement = placement;
        this.expeditionCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();
    }

    @Override
    public @NotNull List<Destination> destinationsFor(@NotNull Player player) {
        final Optional<String> here = siteOf(player);

        final List<Destination> destinations = new ArrayList<>();
        for (Site site : registry.all()) {
            if (!isPort(site) || here.filter(site.getId()::equals).isPresent()) {
                continue;
            }

            final Destination port = destination(site, site.getDisplayName());
            if (port.isReady()) {
                destinations.add(port);
            }
        }

        destinations.addAll(expeditionCache.get(player.getUniqueId(), id -> expeditions()));
        return destinations;
    }

    /** A place that exists whether or not anybody sails to it, and is offered under its own name. */
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

            final Destination offer = destination(site, Component.text(PlaceNames.generate()));
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

    private @NotNull Destination destination(@NotNull Site site, @NotNull Component displayName) {
        return new ShipDestination(site, site.key(), displayName, crewService, voyageService, placement);
    }

    /** The site a player is standing on, if the world they are in belongs to one. */
    private @NotNull Optional<String> siteOf(@NotNull Player player) {
        return instances.byWorld(player.getWorld().getName()).map(instance -> instance.getKey().getSiteId());
    }
}
