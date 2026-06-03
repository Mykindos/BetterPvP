package me.mykindos.betterpvp.core.world.zone.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.world.zone.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns zone-discovery state and behaviour: tracks which zones each online player has already discovered, shows the
 * first-visit notification, and persists discoveries via {@link ZoneDiscoveryRepository}.
 * <p>
 * The per-player discovered set is preloaded on {@code AsyncClientLoadEvent} (see {@code ZoneDiscoveryListener}); a
 * player with no loaded set yet is treated as "not ready" and skipped, so a slow DB load can never produce a false
 * first-visit notification.
 */
@Singleton
@CustomLog
public class ZoneDiscoveryService {

    private final ZoneDiscoveryRepository repository;
    private final ClientManager clientManager;

    /** zone keys each online player has discovered; absence of an entry means "not loaded yet". */
    private final Map<UUID, Set<String>> discovered = new ConcurrentHashMap<>();
    /** zone key → persisted zone id, warmed from loads and upserts to avoid re-resolving the id. */
    private final Map<String, Long> zoneIdByKey = new ConcurrentHashMap<>();

    @Inject
    public ZoneDiscoveryService(ZoneDiscoveryRepository repository, ClientManager clientManager) {
        this.repository = repository;
        this.clientManager = clientManager;
    }

    /**
     * Preloads a client's discovered zones into memory. Intended to run on the async client-load path; it blocks on the
     * repository read (which itself has a timeout and fallback). Always installs a (possibly empty) set so the client
     * counts as "loaded".
     */
    public void loadForClient(@NotNull Client client) {
        final Set<String> set = ConcurrentHashMap.newKeySet();
        try {
            final Map<String, Long> loaded = repository.loadForClient(client.getId()).join();
            zoneIdByKey.putAll(loaded);
            set.addAll(loaded.keySet());
        } catch (Exception ex) {
            log.error("Failed to preload zone discoveries for {}", client.getName(), ex).submit();
        }
        discovered.put(client.getUniqueId(), set);
    }

    /**
     * Handles a player entering a zone: if the zone is discoverable and the player has not discovered it before, shows
     * the notification immediately, schedules persistence, and fires {@link ZoneDiscoveredEvent}.
     */
    public void discover(@NotNull Player player, @NotNull Zone zone) {
        if (!zone.isDiscoverable()) {
            return;
        }
        final Set<String> set = discovered.get(player.getUniqueId());
        if (set == null) {
            return; // discoveries not loaded yet — skip rather than risk a false first-visit
        }
        final String key = zone.getKey().asString();
        if (!set.add(key)) {
            return; // already discovered
        }

        notifyDiscovery(player, zone);

        final Client client = clientManager.search().online(player);
        persist(client.getId(), key, plain(zone.getDisplayName()));
        UtilServer.callEvent(new ZoneDiscoveredEvent(player, zone));
    }

    /**
     * Drops a player's cached discoveries (on unload).
     */
    public void clear(@NotNull UUID uuid) {
        discovered.remove(uuid);
    }

    // <editor-fold desc="Admin operations (used by /zone discovery, no notification)">

    public CompletableFuture<List<ZoneDiscoveryRecord>> list(@NotNull Client target) {
        return repository.listForClient(target.getId());
    }

    public CompletableFuture<Void> addManual(@NotNull Client target, @NotNull Zone zone) {
        final String key = zone.getKey().asString();
        final Set<String> set = discovered.get(target.getUniqueId());
        if (set != null) {
            set.add(key);
        }
        return repository.ensureZoneId(key, plain(zone.getDisplayName())).thenCompose(zoneId -> {
            if (zoneId == null) {
                return CompletableFuture.completedFuture(null);
            }
            zoneIdByKey.put(key, zoneId);
            return repository.insertDiscovery(target.getId(), zoneId);
        });
    }

    public CompletableFuture<Void> remove(@NotNull Client target, @NotNull Zone zone) {
        final String key = zone.getKey().asString();
        final Set<String> set = discovered.get(target.getUniqueId());
        if (set != null) {
            set.remove(key);
        }
        return repository.deleteForClientAndKey(target.getId(), key);
    }

    public CompletableFuture<Void> reset(@NotNull Client target) {
        final Set<String> set = discovered.get(target.getUniqueId());
        if (set != null) {
            set.clear();
        }
        return repository.deleteAllForClient(target.getId());
    }

    // </editor-fold>

    private void persist(long clientId, String zoneKey, String displayName) {
        repository.ensureZoneId(zoneKey, displayName).thenAccept(zoneId -> {
            if (zoneId == null) {
                return;
            }
            zoneIdByKey.put(zoneKey, zoneId);
            repository.insertDiscovery(clientId, zoneId);
        });
    }

    private void notifyDiscovery(@NotNull Player player, @NotNull Zone zone) {
        final ZoneDiscovery discovery = Objects.requireNonNull(zone.getDiscovery());
        final Gamer gamer = clientManager.search().online(player).getGamer();

        final Component subtitle = discovery.getSubtitle() != null ? discovery.getSubtitle() : defaultSubtitle(zone);
        gamer.getTitleQueue().add(-100, new TitleComponent(discovery.getFadeIn(), discovery.getStay(),
                discovery.getFadeOut(), true,
                g -> discovery.getTitle(),
                g -> subtitle));

        if (discovery.getSound() != null) {
            discovery.getSound().play(player);
        }

        final Component message = discovery.getMessage() != null ? discovery.getMessage() : defaultMessage(zone);
        UtilMessage.message(player, message);
    }

    /** The zone's display name in white with no decoration. */
    private static Component defaultSubtitle(@NotNull Zone zone) {
        return Component.text(plain(zone.getDisplayName()), NamedTextColor.WHITE);
    }

    /** A blank-padded "Unlocked area: &lt;zone&gt;" chat line. */
    private static Component defaultMessage(@NotNull Zone zone) {
        return Component.empty()
                .appendNewline()
                .append(Component.text("Unlocked area: ", NamedTextColor.YELLOW))
                .append(zone.getDisplayName())
                .appendNewline();
    }

    private static String plain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
