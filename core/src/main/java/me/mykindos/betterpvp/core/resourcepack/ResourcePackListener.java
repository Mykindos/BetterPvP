package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sends the release's packs while the player is still in the configuration phase, so they load before the world, and
 * turns the player away when a pack is declined or fails. Off on servers behind a proxy that sends the packs itself.
 */
@CustomLog
@Singleton
@BPvPListener
public class ResourcePackListener implements Listener {

    private final ResourcePackService service;

    @Inject
    @Config(path = "core.resourcepack.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    public ResourcePackListener(ResourcePackService service) {
        this.service = service;
    }

    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent event) {
        if (!enabled) {
            return;
        }
        final PackRelease release = service.current().orElse(null);
        if (release == null) {
            return;
        }
        final ResourcePackStatus failure = sendAndWait(event.getConnection().getAudience(), release.getPacks());
        if (failure == ResourcePackStatus.DECLINED) {
            event.getConnection().disconnect(Translations.component("core.resourcepack.declined"));
        } else if (failure != null) {
            event.getConnection().disconnect(Translations.component("core.resourcepack.failed"));
        }
    }

    /**
     * Sends packs and blocks until each reaches a final status. Returns the first status that is not a successful
     * load, or null when they all loaded.
     */
    private ResourcePackStatus sendAndWait(Audience audience, List<ReleasedPack> packs) {
        final Set<UUID> pending = ConcurrentHashMap.newKeySet();
        packs.forEach(pack -> pending.add(pack.getId()));
        final CompletableFuture<ResourcePackStatus> done = new CompletableFuture<>();
        audience.sendResourcePacks(request(packs, true, (id, status) -> {
            if (status != ResourcePackStatus.SUCCESSFULLY_LOADED) {
                done.complete(status);
            } else if (pending.remove(id) && pending.isEmpty()) {
                done.complete(null);
            }
        }));
        try {
            return done.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException ex) {
            return ResourcePackStatus.FAILED_DOWNLOAD;
        } catch (Exception ex) {
            log.error("Waiting for resource packs failed", ex).submit();
            return ResourcePackStatus.FAILED_DOWNLOAD;
        }
    }

    private ResourcePackRequest request(List<ReleasedPack> packs, boolean replace, PackStatusHandler handler) {
        final List<ResourcePackInfo> infos = packs.stream().map(ReleasedPack::toInfo).toList();
        return ResourcePackRequest.resourcePackRequest()
                .packs(infos)
                .replace(replace)
                .required(true)
                .prompt(Translations.component("core.resourcepack.prompt"))
                .callback((id, status, audience) -> {
                    if (!status.intermediate()) {
                        handler.handle(id, status);
                    }
                })
                .build();
    }

    /**
     * Sends online players the packs a new release changed and takes away the packs it dropped, so a release reaches
     * players without a rejoin.
     */
    @EventHandler
    public void onReleaseChanged(PackReleaseChangedEvent event) {
        if (!enabled || event.getPrevious() == null) {
            return;
        }
        final Map<UUID, ReleasedPack> before = event.getPrevious().getPacks().stream()
                .collect(Collectors.toMap(ReleasedPack::getId, Function.identity()));
        final List<ReleasedPack> changed = new ArrayList<>();
        for (ReleasedPack pack : event.getCurrent().getPacks()) {
            final ReleasedPack old = before.remove(pack.getId());
            if (old == null || !Objects.equals(old.getSha1(), pack.getSha1())) {
                changed.add(pack);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            before.keySet().forEach(player::removeResourcePacks);
            if (!changed.isEmpty()) {
                player.sendResourcePacks(request(changed, false, (id, status) -> {
                    if (status == ResourcePackStatus.DECLINED || status == ResourcePackStatus.FAILED_DOWNLOAD) {
                        log.warn("{} did not load pack {} ({})", player.getName(), id, status).submit();
                    }
                }));
            }
        }
    }

    public void resend(Player player) {
        service.current().ifPresent(release -> player.sendResourcePacks(request(release.getPacks(), true, (id, status) -> {
        })));
    }

    @FunctionalInterface
    private interface PackStatusHandler {
        void handle(UUID id, ResourcePackStatus status);
    }

}
