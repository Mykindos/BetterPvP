package me.mykindos.betterpvp.clans.world.island;

import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link Destination} representing an unallocated discovery island slot: a template plus a generated identity, with
 * no world provisioned yet. Allocation happens lazily in {@link #receive(Player)} so that browsing the navigator
 * never clones a world folder nobody ends up visiting. Where the instance comes from and how the player physically
 * gets there are delegated to {@link IslandAllocator} and {@link TravelTransport} respectively, so this class does
 * not need to know or care whether either happens locally or on another server.
 */
@CustomLog
public class IslandOffer implements Destination {

    private final IslandTemplate template;
    private final IslandInstanceManager instanceManager;
    private final IslandAllocationTracker allocationTracker;
    private final IslandAllocator allocator;
    private final TravelTransport transport;
    private final int maxInstances;
    private final Key key;
    private final Component displayName;
    private final ItemView icon;

    public IslandOffer(@NotNull IslandTemplate template, @NotNull String placeName, @NotNull IslandInstanceManager instanceManager,
                        @NotNull IslandAllocationTracker allocationTracker, @NotNull IslandAllocator allocator,
                        @NotNull TravelTransport transport, int maxInstances) {
        this.template = template;
        this.instanceManager = instanceManager;
        this.allocationTracker = allocationTracker;
        this.allocator = allocator;
        this.transport = transport;
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
        return liveInstanceCount() < maxInstances;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller) {
        if (!allocationTracker.begin(traveller)) {
            return CompletableFuture.completedFuture(false);
        }

        return allocator.allocate(template).thenCompose(handle -> {
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

    private long liveInstanceCount() {
        return instanceManager.all().stream()
                .filter(instance -> instance.getTemplate().getKey().equals(template.getKey()))
                .count();
    }
}
