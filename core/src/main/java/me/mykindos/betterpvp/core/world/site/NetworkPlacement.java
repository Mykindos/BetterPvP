package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.framework.net.PlayerTransfer;
import me.mykindos.betterpvp.core.framework.net.RemoteInstance;
import me.mykindos.betterpvp.core.framework.net.SiteDirectory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Placement across a network. Finds a party an instance wherever it lives, and moves players between servers to reach
 * it. Nothing above this changes between one server and twenty.
 * <p>
 * Locating resolves in a fixed order: an instance this server already holds, then one another server holds with room,
 * then a new one on whichever server is emptiest. A site pinned to a server by its policy skips all of that.
 */
@BPvPListener
@Singleton
@CustomLog
public class NetworkPlacement implements Placement, Listener {

    private final Core core;
    private final SiteRegistry registry;
    private final SiteInstances instances;
    private final SiteDirectory directory;
    private final PlayerTransfer transfers;
    private final LocalPlacement local;

    @Inject
    public NetworkPlacement(@NotNull Core core, @NotNull SiteRegistry registry, @NotNull SiteInstances instances,
                            @NotNull SiteDirectory directory, @NotNull PlayerTransfer transfers,
                            @NotNull LocalPlacement local) {
        this.core = core;
        this.registry = registry;
        this.instances = instances;
        this.directory = directory;
        this.transfers = transfers;
        this.local = local;
    }

    @Override
    public @NotNull String hostFor(@NotNull Site site) {
        final String pinned = site.getPolicy().getServer();
        return pinned == null ? currentServer() : pinned;
    }

    @Override
    public boolean isLocal(@NotNull Site site) {
        return hostFor(site).equals(currentServer());
    }

    @Override
    public @NotNull CompletableFuture<SiteHandle> locate(@NotNull SiteKey key, @NotNull Party party) {
        final Optional<Site> found = registry.get(key.getSiteId());
        if (found.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("No such site: " + key));
        }

        final Site site = found.get();
        if (site.getPolicy().getServer() != null) {
            return onHost(site, key, party, site.getPolicy().getServer());
        }

        if (key.isOwned()) {
            // An owned world lives on one machine's disk, so the first server to claim it keeps it.
            return directory.stickyHost(key.getSiteId(), key.getOwnerId(), currentServer())
                    .thenCompose(host -> onHost(site, key, party, host));
        }

        return directory.lookup(key.getSiteId(), key.getOwnerId())
                .thenCompose(known -> joinExisting(site, key, party, known))
                .thenCompose(handle -> handle.isPresent()
                        ? CompletableFuture.completedFuture(handle.get())
                        : provisionSomewhere(site, key, party));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> send(@NotNull Player traveller, @NotNull SiteHandle handle) {
        return sendAll(List.of(traveller), handle);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> sendAll(@NotNull Collection<Player> travellers,
                                                        @NotNull SiteHandle handle) {
        if (travellers.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        if (handle.getServer().equals(currentServer())) {
            return local.sendAll(travellers, handle);
        }

        final RemoteInstance destination = new RemoteInstance(handle.getInstanceId(), handle.getKey().getSiteId(),
                handle.getKey().getOwnerId(), handle.getServer(), handle.getWorld(), true, 0);

        // Recorded before the transfer, because the server they land on has no other way to know why they came.
        travellers.forEach(traveller -> directory.expect(traveller.getUniqueId(), destination));
        return transfers.transferAll(travellers, handle.getServer());
    }

    /**
     * Puts a player who has just arrived from another server into the instance they were sent for.
     * <p>
     * They land wherever this server would normally have put them, which is why this then moves them. Residency has
     * already run by now and may have placed them somewhere of its own, so this deliberately runs afterwards and
     * wins: being sent here for a site is a stronger claim than where they last logged out.
     */
    @EventHandler
    public void onJoin(@NotNull ClientJoinEvent event) {
        final Player player = event.getPlayer();

        directory.claimArrival(player.getUniqueId()).thenAccept(arrival -> arrival.ifPresent(instance ->
                UtilServer.runTask(core, () -> land(player, instance))));
    }

    private void land(@NotNull Player player, @NotNull RemoteInstance instance) {
        final Optional<Site> site = registry.get(instance.getSiteId());
        if (site.isEmpty()) {
            log.warn("{} arrived for site '{}', which this server does not have", player.getName(), instance.getSiteId()).submit();
            return;
        }

        final SiteKey key = instance.getOwnerId() == SiteKey.NO_OWNER
                ? SiteKey.of(instance.getSiteId())
                : SiteKey.of(instance.getSiteId(), instance.getOwnerId());

        instances.locate(key, Party.solo(player.getUniqueId()))
                .thenCompose(located -> local.send(player, handleFor(key, located)))
                // The seat held for this player while they travelled is theirs now, counted as an occupant by the
                // server they are standing on, so holding it in the directory as well would count them twice.
                .whenComplete((placed, error) -> directory.release(instance.getId(), 1))
                .exceptionally(error -> {
                    log.error("Could not put {} into site {} after they arrived", player.getName(), key, error).submit();
                    return false;
                });
    }

    /** An instance this server holds, or one somewhere else that will take the party. */
    private @NotNull CompletableFuture<Optional<SiteHandle>> joinExisting(@NotNull Site site, @NotNull SiteKey key,
                                                                          @NotNull Party party,
                                                                          @NotNull List<RemoteInstance> known) {
        final List<RemoteInstance> usable = known.stream()
                .filter(RemoteInstance::isReady)
                .filter(instance -> hasRoom(instance, site, party))
                .toList();

        // This server first, so a party that could stay puts nobody through a transfer.
        final Optional<RemoteInstance> here = usable.stream()
                .filter(instance -> instance.isLocal(currentServer()))
                .findFirst();
        if (here.isPresent()) {
            return instances.locate(key, party).thenApply(located -> Optional.of(handleFor(key, located)));
        }

        final Optional<RemoteInstance> elsewhere = usable.stream().findFirst();
        if (elsewhere.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final RemoteInstance chosen = elsewhere.get();
        return directory.reserve(chosen.getId(), party.size(), site.getPolicy().getCapacity())
                .thenApply(reserved -> reserved
                        ? Optional.of(new SiteHandle(chosen.getId(), key, chosen.getServer(), chosen.getWorld()))
                        : Optional.empty());
    }

    /** Makes a new instance, here if this server is the emptiest and on that server otherwise. */
    private @NotNull CompletableFuture<SiteHandle> provisionSomewhere(@NotNull Site site, @NotNull SiteKey key,
                                                                      @NotNull Party party) {
        return directory.serversByLoad().thenCompose(servers -> {
            final String emptiest = servers.isEmpty() ? currentServer() : servers.get(0);
            return onHost(site, key, party, emptiest);
        });
    }

    /**
     * Locates on one named server. Locally that is the ordinary path; remotely it is a promise that the server will
     * have an instance by the time the party lands, which it keeps because their arrival makes it locate one.
     */
    private @NotNull CompletableFuture<SiteHandle> onHost(@NotNull Site site, @NotNull SiteKey key,
                                                          @NotNull Party party, @NotNull String host) {
        if (host.equals(currentServer())) {
            return instances.locate(key, party).thenApply(located -> handleFor(key, located));
        }

        return directory.lookup(key.getSiteId(), key.getOwnerId()).thenApply(known -> known.stream()
                .filter(instance -> instance.getServer().equals(host))
                .filter(instance -> hasRoom(instance, site, party))
                .findFirst()
                .map(instance -> new SiteHandle(instance.getId(), key, host, instance.getWorld()))
                // Nothing of this site is up over there yet. The handle names the server anyway, and the party is
                // sent to it: what they need is an instance when they get there, not one before they leave.
                .orElseGet(() -> new SiteHandle(SiteHandle.PENDING, key, host, "")));
    }

    private boolean hasRoom(@NotNull RemoteInstance instance, @NotNull Site site, @NotNull Party party) {
        final int capacity = site.getPolicy().getCapacity();
        return capacity <= 0 || instance.getOccupants() + party.size() <= capacity;
    }

    private @NotNull SiteHandle handleFor(@NotNull SiteKey key, @NotNull SiteInstance instance) {
        return new SiteHandle(instance.getId(), key, currentServer(), instance.getWorldName());
    }

    private @NotNull String currentServer() {
        return Core.getCurrentRealm().getServer().getName();
    }
}
