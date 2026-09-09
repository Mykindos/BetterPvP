package me.mykindos.betterpvp.clans.world.island;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.voyage.VoyageService;
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
 * Generates the discovery island offers shown at a helm. Each offer is an unallocated {@link IslandOffer} — no world
 * is cloned until a crew actually sails to one and gets there. Offers are cached per player for a short window so the
 * menu does not reshuffle mid-interaction, and are regenerated with fresh place-names once that window expires.
 */
@Singleton
public class IslandOfferProvider implements DestinationProvider {

    private final IslandTemplateRegistry templateRegistry;
    private final IslandInstanceManager instanceManager;
    private final IslandAllocationTracker allocationTracker;
    private final IslandAllocator allocator;
    private final TravelTransport transport;
    private final CrewService crewService;
    private final VoyageService voyageService;
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
                               @NotNull TravelTransport transport,
                               @NotNull CrewService crewService, @NotNull VoyageService voyageService) {
        this.allocationTracker = allocationTracker;
        this.templateRegistry = templateRegistry;
        this.instanceManager = instanceManager;
        this.allocator = allocator;
        this.transport = transport;
        this.crewService = crewService;
        this.voyageService = voyageService;
        this.offerCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public @NotNull List<Destination> destinationsFor(@NotNull Player player) {
        return offerCache.get(player.getUniqueId(), id -> generateOffers());
    }

    private @NotNull List<Destination> generateOffers() {
        final List<Destination> offers = new ArrayList<>();
        for (IslandTemplate template : servableTemplates()) {
            if (offers.size() >= offerCount) {
                break;
            }

            final IslandOffer offer = offerFor(template);
            if (offer.isReady()) {
                offers.add(offer);
            }
        }

        return offers;
    }

    private @NotNull IslandOffer offerFor(@NotNull IslandTemplate template) {
        return new IslandOffer(template, IslandPlaceNames.generate(), instanceManager, allocationTracker,
                allocator, transport, crewService, voyageService, maxInstances);
    }

    /** Everywhere a player can actually be sent, in a fresh order each time. */
    private @NotNull List<IslandTemplate> servableTemplates() {
        final List<IslandTemplate> candidates = new ArrayList<>();
        for (IslandTemplate template : templateRegistry.all()) {
            // Not every template is a place to visit. The open sea a crew crosses is allocated the same way and lives
            // in the same registry, but offering it would sell passage to an empty ocean.
            if (!VoyageService.LIMBO_TEMPLATE.equals(template.getKey())) {
                candidates.add(template);
            }
        }
        Collections.shuffle(candidates);
        return candidates;
    }
}
