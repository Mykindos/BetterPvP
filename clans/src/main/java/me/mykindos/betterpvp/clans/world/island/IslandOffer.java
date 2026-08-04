package me.mykindos.betterpvp.clans.world.island;

import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.clans.world.voyage.Landfall;
import me.mykindos.betterpvp.clans.world.voyage.VoyageService;
import me.mykindos.betterpvp.clans.world.voyage.VoyageTiming;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link Destination} representing an unallocated discovery island slot: a template plus a generated identity, with
 * no world provisioned yet.
 * <p>
 * Reached by sea like anywhere else a helm can set a course for. Clicking it in the navigator does not teleport
 * anybody — it puts the whole crew out on the open water for the crossing, and the island itself is not provisioned
 * until they sight it. Allocating on arrival rather than on departure is what keeps a cloned world from standing empty
 * for the length of a voyage, where the reaper would find it and give it back.
 * <p>
 * Where the instance comes from and how the crew physically gets to it are delegated to {@link IslandAllocator} and
 * {@link TravelTransport}, so this class does not need to know or care whether either happens locally or on another
 * server.
 */
@CustomLog
public class IslandOffer implements Destination, Landfall {

    private final IslandTemplate template;
    private final IslandInstanceManager instanceManager;
    private final IslandAllocationTracker allocationTracker;
    private final IslandAllocator allocator;
    private final TravelTransport transport;
    private final CrewService crewService;
    private final VoyageService voyageService;
    private final int maxInstances;
    private final Key key;
    private final Component displayName;
    private final ItemView icon;

    public IslandOffer(@NotNull IslandTemplate template, @NotNull String placeName, @NotNull IslandInstanceManager instanceManager,
                        @NotNull IslandAllocationTracker allocationTracker, @NotNull IslandAllocator allocator,
                        @NotNull TravelTransport transport, @NotNull CrewService crewService,
                        @NotNull VoyageService voyageService, int maxInstances) {
        this.template = template;
        this.instanceManager = instanceManager;
        this.allocationTracker = allocationTracker;
        this.allocator = allocator;
        this.transport = transport;
        this.crewService = crewService;
        this.voyageService = voyageService;
        this.maxInstances = maxInstances;
        this.key = Key.key("betterpvp", "island-offer/" + template.getKey() + "/" + UUID.randomUUID());
        this.displayName = Component.text(placeName);
        this.icon = ItemView.builder()
                .material(template.getIcon())
                .displayName(this.displayName)
                .build();
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
    public boolean isReady() {
        return voyageService.canDepart() && liveInstanceCount() < maxInstances;
    }

    /** A course is set here, not walked; the crew is welcomed when they make landfall. */
    @Override
    public boolean announcesArrival() {
        return false;
    }

    @Override
    public @NotNull VoyageTiming timing() {
        return template.getTiming();
    }

    /** The island this offer would take a traveller to. */
    public @NotNull IslandTemplate getTemplate() {
        return template;
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
     * Makes the island and puts the crew on it.
     * <p>
     * The captain lands first and the rest follow only once they are there, because sharing works off occupancy: an
     * instance is offered to a sailor because a crewmate is already standing in it. Delivered all at once, nobody would
     * yet be, and a crew of four would arrive on four separate copies of the same island.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> setAshore(@NotNull List<Player> sailors) {
        if (sailors.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return deliver(sailors.getFirst()).thenCompose(landed -> {
            if (!Boolean.TRUE.equals(landed)) {
                return CompletableFuture.completedFuture(false);
            }

            final List<CompletableFuture<Boolean>> following = new ArrayList<>();
            for (Player sailor : sailors.subList(1, sailors.size())) {
                following.add(deliver(sailor));
            }
            return CompletableFuture.allOf(following.toArray(CompletableFuture[]::new)).thenApply(ignored -> true);
        });
    }

    /** Puts one sailor on an instance of this island, allocating one if there is none they may join. */
    private @NotNull CompletableFuture<Boolean> deliver(@NotNull Player traveller) {
        if (!allocationTracker.begin(traveller)) {
            return CompletableFuture.completedFuture(false);
        }

        return handleFor(traveller).thenCompose(handle -> {
            final Optional<IslandInstance> instanceOptional = instanceManager.find(handle.getInstanceId());
            if (instanceOptional.isEmpty()) {
                log.warn("Allocated island handle {} but no local instance is registered for it", handle.getInstanceId()).submit();
                return CompletableFuture.completedFuture(false);
            }

            final IslandInstance instance = instanceOptional.get();
            instanceManager.enter(instance, traveller);

            // A failed arrival must give the occupancy back, or the instance stays occupied forever and is never reaped.
            return transport.deliver(traveller, handle).thenApply(arrived -> {
                if (!Boolean.TRUE.equals(arrived)) {
                    instanceManager.exit(instance, traveller);
                }
                return arrived;
            });
        }).exceptionally(ex -> {
            log.warn("Failed to allocate island instance for template {}", template.getKey(), ex).submit();
            return false;
        }).whenComplete((result, ex) -> allocationTracker.finish(traveller));
    }

    /**
     * An instance of this template the allocation policy will let {@code traveller} join, or a freshly allocated one.
     * <p>
     * Sharing is what lands a crew together: without it every sailor allocates their own copy of the island and a
     * group that sailed as one arrives alone. A shareable instance is by definition already running here, so it needs
     * no allocation and no routing.
     */
    private @NotNull CompletableFuture<IslandHandle> handleFor(@NotNull Player traveller) {
        return instanceManager.offerTo(traveller)
                .filter(instance -> instance.getTemplate().getKey().equals(template.getKey()))
                .map(instance -> CompletableFuture.completedFuture(new IslandHandle(instance.getId(),
                        template.getKey(), Core.getCurrentRealm().getServer().getName(), instance.getWorldName())))
                .orElseGet(() -> allocator.allocate(template));
    }

    private long liveInstanceCount() {
        return instanceManager.all().stream()
                .filter(instance -> instance.getTemplate().getKey().equals(template.getKey()))
                .count();
    }
}
