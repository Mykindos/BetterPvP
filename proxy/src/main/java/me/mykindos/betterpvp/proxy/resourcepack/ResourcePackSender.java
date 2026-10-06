package me.mykindos.betterpvp.proxy.resourcepack;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.configuration.PlayerFinishConfigurationEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends the release's packs the first time a player configures through the proxy, so they stay loaded across
 * server switches, and turns the player away when a pack is declined or fails. A new release reaches online players
 * without a rejoin.
 */
public final class ResourcePackSender {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final PackReleaseFeed feed;
    /**
     * Players sent the packs on this connection, with the ids of the packs still loading at join.
     */
    private final Map<UUID, Set<UUID>> pendingAtJoin = new ConcurrentHashMap<>();

    public ResourcePackSender(ProxyServer proxyServer, Logger logger, PackReleaseFeed feed) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.feed = feed;
    }

    @Subscribe
    public void onFinishConfiguration(PlayerFinishConfigurationEvent event) {
        final Player player = event.player();
        if (pendingAtJoin.containsKey(player.getUniqueId())) {
            return;
        }
        feed.current().ifPresent(release -> {
            final Set<UUID> pending = ConcurrentHashMap.newKeySet();
            release.getPacks().forEach(pack -> pending.add(pack.getId()));
            pendingAtJoin.put(player.getUniqueId(), pending);
            player.sendResourcePacks(request(release.getPacks(), true));
        });
    }

    @Subscribe
    public void onStatus(PlayerResourcePackStatusEvent event) {
        final PlayerResourcePackStatusEvent.Status status = event.getStatus();
        if (status.isIntermediate()) {
            return;
        }
        final Player player = event.getPlayer();
        final Set<UUID> pending = pendingAtJoin.get(player.getUniqueId());
        if (pending == null || !pending.remove(event.getPackId())) {
            if (status != PlayerResourcePackStatusEvent.Status.SUCCESSFUL) {
                logger.warn("{} did not load pack {} ({})", player.getUsername(), event.getPackId(), status);
            }
            return;
        }
        if (status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            player.disconnect(Component.translatable("core.resourcepack.declined"));
        } else if (status != PlayerResourcePackStatusEvent.Status.SUCCESSFUL) {
            player.disconnect(Component.translatable("core.resourcepack.failed"));
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        pendingAtJoin.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Sends online players the packs a new release changed and takes away the packs it dropped.
     */
    public void onReleaseChanged(PackRelease previous, PackRelease current) {
        if (previous == null) {
            return;
        }
        final Map<UUID, ReleasedPack> before = new HashMap<>();
        previous.getPacks().forEach(pack -> before.put(pack.getId(), pack));
        final List<ReleasedPack> changed = new ArrayList<>();
        for (ReleasedPack pack : current.getPacks()) {
            final ReleasedPack old = before.remove(pack.getId());
            if (old == null || !Objects.equals(old.getSha1(), pack.getSha1())) {
                changed.add(pack);
            }
        }
        for (Player player : proxyServer.getAllPlayers()) {
            if (!pendingAtJoin.containsKey(player.getUniqueId())) {
                continue;
            }
            before.keySet().forEach(player::removeResourcePacks);
            if (!changed.isEmpty()) {
                player.sendResourcePacks(request(changed, false));
            }
        }
    }

    private ResourcePackRequest request(List<ReleasedPack> packs, boolean replace) {
        final List<ResourcePackInfo> infos = packs.stream().map(ReleasedPack::toInfo).toList();
        return ResourcePackRequest.resourcePackRequest()
                .packs(infos)
                .replace(replace)
                .required(true)
                .prompt(Component.translatable("core.resourcepack.prompt"))
                .build();
    }

}
