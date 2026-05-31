package me.mykindos.betterpvp.clans.clans.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.ZoneProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Bridges clan territory into the core {@link ZoneManager}: resolves a location to its owning clan via the existing
 * O(1) chunk-PDC lookup ({@link ClanManager#getClanByChunk}) and returns that clan's territory {@link Zone}. The chunk
 * PDC remains the runtime source of truth; this provider simply exposes it through the zone framework, so
 * {@code zoneManager.getZoneAt(location)} and the player enter/exit events now cover clan land.
 * <p>
 * Per-clan zones are built lazily and cached by clan id; the cache is evicted on disband. The zone's bounds delegate to
 * the live clan, so claiming/unclaiming needs no zone rebuild. Territory protection is enforced by handling
 * {@link me.mykindos.betterpvp.core.world.zone.ZoneInteractEvent} on these zones (see {@code ClansWorldListener}).
 */
@BPvPListener
@Singleton
public class ClanZoneProvider implements ZoneProvider, Listener {

    private final ClanManager clanManager;
    private final Map<Long, Zone> zones = new ConcurrentHashMap<>();

    @Inject
    private ClanZoneProvider(ClanManager clanManager, ZoneManager zoneManager) {
        this.clanManager = clanManager;
        zoneManager.addProvider(this);
    }

    @Override
    public @NotNull Stream<Zone> zonesAt(@NotNull Location location) {
        return clanManager.getClanByChunk(location.getChunk())
                .stream()
                .map(this::zoneFor);
    }

    /**
     * @param clan the clan
     * @return the cached zone representing the clan's territory, building it on first request
     */
    public @NotNull Zone zoneFor(@NotNull Clan clan) {
        return zones.computeIfAbsent(clan.getId(), id -> Zone.builder()
                .key(Key.key("clans", "territory_" + id))
                .displayName(Component.text(clan.getName()))
                .bounds(new ClanTerritoryBounds(clan))
                .priority(ClanZones.CLAN_TERRITORY_PRIORITY)
                .tag(ClanZones.TERRITORY)
                .build());
    }

    @EventHandler
    public void onDisband(ClanDisbandEvent event) {
        zones.remove(event.getClan().getId());
    }
}
