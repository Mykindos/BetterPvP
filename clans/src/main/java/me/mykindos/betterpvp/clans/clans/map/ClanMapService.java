package me.mykindos.betterpvp.clans.clans.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.map.claim.ClanClaimIndex;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves what a given viewer's map should say about other clans, and hands out map items.
 * <p>
 * A viewer contributes only a colour per clan, since the claims themselves live once in {@link ClanClaimIndex}. All
 * this holds per viewer is a small relation table, built on demand and invalidated on the clan events that can change
 * it, so joining and claiming cost no work proportional to players times claims.
 */
@Singleton
public class ClanMapService {

    private final MapHandler mapHandler;
    private final ClanManager clanManager;
    private final ClanClaimIndex claimIndex;

    private final Map<UUID, Long2ObjectMap<ClanRelation>> relations = new ConcurrentHashMap<>();
    private final Map<UUID, Long2ObjectMap<MapColor>> colours = new ConcurrentHashMap<>();

    @Inject
    public ClanMapService(MapHandler mapHandler, ClanManager clanManager, ClanClaimIndex claimIndex) {
        this.mapHandler = mapHandler;
        this.clanManager = clanManager;
        this.claimIndex = claimIndex;
    }

    /**
     * @param player the viewer
     * @return that viewer's relation to every clan with an id, keyed by clan id. Built on demand and cached; call from
     * the main thread, since it reads live clan relations.
     */
    public @NotNull Long2ObjectMap<ClanRelation> relations(@NotNull Player player) {
        return relations.computeIfAbsent(player.getUniqueId(), ignored -> {
            final Clan viewerClan = clanManager.getClanByPlayer(player).orElse(null);
            final Long2ObjectMap<ClanRelation> table = new Long2ObjectOpenHashMap<>();
            for (Clan clan : clanManager.getObjects().values()) {
                table.put(clan.getId(), clanManager.getRelation(viewerClan, clan));
            }
            return table;
        });
    }

    /**
     * @param player the viewer
     * @return the map colour to paint each clan's claims in, keyed by clan id
     */
    public @NotNull Long2ObjectMap<MapColor> claimColours(@NotNull Player player) {
        return colours.computeIfAbsent(player.getUniqueId(), ignored -> {
            final Long2ObjectMap<MapColor> table = new Long2ObjectOpenHashMap<>();
            for (Long2ObjectMap.Entry<ClanRelation> entry : relations(player).long2ObjectEntrySet()) {
                table.put(entry.getLongKey(), entry.getValue().getMaterialColor());
            }
            return table;
        });
    }

    /**
     * Drops a viewer's cached relation table and forces their next frame to be rebuilt.
     */
    public void invalidate(@NotNull UUID playerId) {
        relations.remove(playerId);
        colours.remove(playerId);
        final MapSettings settings = mapHandler.getMapSettingsMap().get(playerId);
        if (settings != null) {
            settings.setForceRedraw(true);
        }
    }

    /**
     * Drops every viewer's cached relation table, e.g. after a disband changes what everyone sees.
     */
    public void invalidateAll() {
        relations.clear();
        colours.clear();
        mapHandler.getMapSettingsMap().values().forEach(settings -> settings.setForceRedraw(true));
    }

    /**
     * Marks the claim geometry stale. The index rebuild is debounced, so a clan claiming a run of chunks pays for one.
     */
    public void claimsChanged() {
        claimIndex.scheduleRebuild();
    }

    public void removePlayerMapData(@NotNull Player player) {
        final UUID playerId = player.getUniqueId();
        relations.remove(playerId);
        colours.remove(playerId);
        mapHandler.getMapSettingsMap().remove(playerId);
    }

    public @NotNull MapSettings getOrCreateMapSettings(@NotNull Player player) {
        return mapHandler.getOrCreateMapSettings(player);
    }

    /**
     * @return a filled map bound to the shared server map view
     */
    public @NotNull ItemStack createMapItem() {
        final ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        final MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(Bukkit.getMap(0));
        mapItem.setItemMeta(meta);
        return mapItem;
    }
}
