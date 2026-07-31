package me.mykindos.betterpvp.clans.world.island;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.travel.DestinationProvider;
import me.mykindos.betterpvp.core.config.Config;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Generates the discovery island offers shown in the navigator. Each offer is an unallocated {@link IslandOffer} —
 * no world is cloned until a player actually picks one. Offers are cached per player for a short window so the menu
 * does not reshuffle mid-interaction, and are regenerated with fresh place-names once that window expires. Only
 * templates {@link IslandHostRouter} considers servable from this server are offered, so the navigator never
 * advertises an island it cannot deliver.
 */
@Singleton
public class IslandOfferProvider implements DestinationProvider {

    private final IslandTemplateRegistry templateRegistry;
    private final IslandInstanceManager instanceManager;
    private final IslandAllocationTracker allocationTracker;
    private final IslandAllocator allocator;
    private final TravelTransport transport;
    private final IslandHostRouter router;
    private final Cache<UUID, List<Destination>> offerCache;

    @Inject
    @Config(path = "islands.offers.count", defaultValue = "3")
    private int offerCount;

    @Inject
    @Config(path = "islands.offers.max-instances", defaultValue = "10")
    private int maxInstances;

    @Inject
    public IslandOfferProvider(@NotNull IslandTemplateRegistry templateRegistry, @NotNull IslandInstanceManager instanceManager,
                               @NotNull IslandAllocationTracker allocationTracker, @NotNull IslandAllocator allocator,
                               @NotNull TravelTransport transport, @NotNull IslandHostRouter router) {
        this.allocationTracker = allocationTracker;
        this.templateRegistry = templateRegistry;
        this.instanceManager = instanceManager;
        this.allocator = allocator;
        this.transport = transport;
        this.router = router;
        this.offerCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public @NotNull List<Destination> destinationsFor(@NotNull Player player) {
        return offerCache.get(player.getUniqueId(), id -> generateOffers());
    }

    private @NotNull List<Destination> generateOffers() {
        final List<IslandTemplate> candidates = new ArrayList<>();
        for (IslandTemplate template : templateRegistry.all()) {
            if (router.isServable(template)) {
                candidates.add(template);
            }
        }
        Collections.shuffle(candidates);

        final List<Destination> offers = new ArrayList<>();
        for (IslandTemplate template : candidates) {
            if (offers.size() >= offerCount) {
                break;
            }

            final IslandOffer offer = new IslandOffer(template, IslandPlaceNames.generate(), instanceManager, allocationTracker,
                    allocator, transport, maxInstances);
            if (offer.isReady()) {
                offers.add(offer);
            }
        }

        return offers;
    }
}
