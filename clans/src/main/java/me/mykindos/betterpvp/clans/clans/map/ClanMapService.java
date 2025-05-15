package me.mykindos.betterpvp.clans.clans.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.nms.UtilMapMaterial;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ClanMapService class is responsible for managing the map data related to clans,
 * including loading chunks, updating clan claims, modifying player map data, and handling
 * clan relations. It interfaces with the map handler, clan manager, and other utilities
 * to ensure accurate and dynamic updates for players.
 *
 * This service provides functionalities for:
 * - Loading and updating clan chunk data for players dynamically.
 * - Removing and resetting player clan colors from the map.
 * - Handling clan relationships and their visual representation on the map.
 * - Creating and managing map-related settings for individual players.
 */
@Singleton
public class ClanMapService {

    private final Clans clans;
    private final MapHandler mapHandler;
    private final ClanManager clanManager;

    @Inject
    public ClanMapService(Clans clans, MapHandler mapHandler, ClanManager clanManager) {
        this.clans = clans;
        this.mapHandler = mapHandler;
        this.clanManager = clanManager;

        mapHandler.loadMap();
    }

    /**
     * Removes map-related data associated with the specified player.
     *
     * This method removes the player's data from the clan map data and the map settings map
     * maintained by the map handler.
     *
     * @param player the player whose map-related data is to be removed
     */
    public void removePlayerMapData(Player player) {
        mapHandler.getClanMapData().remove(player.getUniqueId());
        mapHandler.getMapSettingsMap().remove(player.getUniqueId());
    }

    /**
     * Loads and updates the player-specific data for clan chunk claims.
     * This method processes the chunks that belong to various clans
     * and associates specific chunk data with the player's unique ID.
     *
     * @param player The player for whom chunk data is being loaded.
     */
    public void loadChunks(Player player) {
        UtilServer.runTaskAsync(clans, () -> {
            Set<ChunkData> chunkClaimColor = mapHandler.getClanMapData().computeIfAbsent(
                    player.getUniqueId(),
                    k -> Collections.newSetFromMap(new ConcurrentHashMap<>())
            );

            Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

            for (Clan clan : clanManager.getObjects().values()) {
                if (clan.getTerritory().isEmpty()) continue;

                MapColor materialColor = getColourForClan(playerClan, clan);

                for (ClanTerritory claim : clan.getTerritory()) {
                    String[] tokens = claim.getChunk().split("/ ");
                    if (tokens.length != 3) continue;

                    int chunkX = Integer.parseInt(tokens[1]);
                    int chunkZ = Integer.parseInt(tokens[2]);

                    ChunkData chunkData = new ChunkData(BPvPWorld.MAIN_WORLD_NAME, materialColor, chunkX, chunkZ, clan);
                    addBordersToChunkData(clan, chunkX, chunkZ, chunkData);
                    chunkClaimColor.add(chunkData);
                }
            }

            updateStatus(player);
        });
    }

    /**
     * Adds border information to the provided ChunkData based on ownership of neighboring chunks.
     *
     * @param clan       The clan associated with the chunk data being processed.
     * @param chunkX     The X-coordinate of the chunk being processed.
     * @param chunkZ     The Z-coordinate of the chunk being processed.
     * @param chunkData  The ChunkData object to which border information will be added.
     */
    private void addBordersToChunkData(Clan clan, int chunkX, int chunkZ, ChunkData chunkData) {
        for (int i = 0; i < 4; i++) {
            BlockFace blockFace = BlockFace.values()[i];
            String targetChunkString = BPvPWorld.MAIN_WORLD_NAME + "/ " +
                    (chunkX + blockFace.getModX()) + "/ " +
                    (chunkZ + blockFace.getModZ());

            if (clan.isChunkOwnedByClan(targetChunkString)) {
                chunkData.getBlockFaceSet().add(blockFace);
            }
        }
    }

    /**
     * Updates the map data for the given clan including their territory claims and
     * related borders. For each online player, the method updates the clan map visualization
     * by removing outdated chunks and adding updated ones for the specified clan.
     *
     * @param clan The clan whose claims need to be updated on the map.
     */
    public void updateClaims(Clan clan) {
        UtilServer.runTaskLaterAsync(clans, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                Clan otherClan = clanManager.getClanByPlayer(online).orElse(null);
                MapColor materialColor = getColourForClan(clan, otherClan);

                // Remove existing chunks for this clan
                mapHandler.getClanMapData()
                        .computeIfAbsent(online.getUniqueId(),
                                k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                        .removeIf(chunkData -> chunkData.getClan().equals(clan));

                // Add updated chunks
                for (ClanTerritory claim : clan.getTerritory()) {
                    String[] tokens = claim.getChunk().split("/ ");
                    if (tokens.length != 3) continue;

                    int chunkX = Integer.parseInt(tokens[1]);
                    int chunkZ = Integer.parseInt(tokens[2]);

                    ChunkData chunkData = new ChunkData(BPvPWorld.MAIN_WORLD_NAME, materialColor, chunkX, chunkZ, clan);
                    addBordersToChunkData(clan, chunkX, chunkZ, chunkData);

                    Set<ChunkData> chunkDataset = mapHandler.getClanMapData().get(online.getUniqueId());
                    if (chunkDataset.stream().noneMatch(cd -> cd.equals(chunkData))) {
                        chunkDataset.add(chunkData);
                    }
                }
                updateStatus(online);
            }
        }, 1);
    }

    /**
     * Updates the clan chunks associated with a specific member.
     * Removes the member's existing clan map data and loads the chunks for the member
     * if they are currently an online player.
     *
     * @param memberId the unique identifier of the member whose clan chunks should be updated
     */
    public void updateClanChunks(UUID memberId) {
        mapHandler.getClanMapData().remove(memberId);
        Player player = Bukkit.getPlayer(memberId);
        if (player != null) {
            loadChunks(player);
        }
    }

    /**
     * Resets the clan map color for all chunks associated with the given player.
     * If a chunk is associated with a non-admin clan, its color is set to neutral.
     * The player's status is updated after resetting the colors.
     *
     * @param player the player whose associated clan map colors need to be reset
     */
    public void resetPlayerClanColors(Player player) {
        for (ChunkData chunkData : mapHandler.getClanMapData()
                .computeIfAbsent(player.getUniqueId(),
                        k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))) {
            final IClan clan = chunkData.getClan();
            if (clan != null && !clan.isAdmin()) {
                chunkData.setColor(UtilMapMaterial.getColorNeutral());
            }
        }
        updateStatus(player);
    }

    /**
     * Removes all chunk data entries associated with the specified clan from the player's clan map.
     *
     * @param player       the player whose clan map entries are to be modified
     * @param clanToRemove the clan to remove from the player's clan map
     */
    public void removeClanFromPlayerMap(Player player, IClan clanToRemove) {
        mapHandler.getClanMapData()
                .computeIfAbsent(player.getUniqueId(),
                        k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .removeIf(chunkData -> clanToRemove.getName().equalsIgnoreCase(chunkData.getClan().getName()));
    }

    /**
     * Updates the clan relations for the provided player by iterating over their associated map data
     * and setting the appropriate color for each chunk based on the relation between the player's clan
     * and the clan owning the respective chunk.
     *
     * @param player the player whose clan relations and map data need to be updated
     */
    public void updatePlayerClanRelations(Player player) {
        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

        for (ChunkData chunkData : mapHandler.getClanMapData().get(player.getUniqueId())) {
            final IClan clan = chunkData.getClan();
            if (clan != null && !clan.isAdmin()) {
                ClanRelation relation = clanManager.getRelation(clan, playerClan);
                chunkData.setColor(relation.getMaterialColor());
            }
        }
    }

    /**
     * Updates the clan colors on the player's map based on the player's clan relations.
     *
     * @param player The player whose clan map colors are being updated.
     * @param playerClan The clan that the player belongs to.
     */
    public void updatePlayerClanColors(Player player, IClan playerClan) {
        Set<ChunkData> chunkData = mapHandler.getClanMapData()
                .computeIfAbsent(player.getUniqueId(),
                        k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));

        chunkData.forEach(cd -> {
            if (cd.getClan().equals(playerClan)) {
                cd.setColor(ClanRelation.SELF.getMaterialColor());
            } else if (playerClan.isAllied(cd.getClan())) {
                Optional<ClanAlliance> clanAllianceOptional = playerClan.getAlliance(cd.getClan());
                clanAllianceOptional.ifPresent(clanAlliance -> {
                    cd.setColor(clanAlliance.isTrusted() ?
                            ClanRelation.ALLY_TRUST.getMaterialColor() :
                            ClanRelation.ALLY.getMaterialColor());
                });
            } else if (playerClan.isEnemy(cd.getClan())) {
                cd.setColor(ClanRelation.ENEMY.getMaterialColor());
            }
        });
    }

    /**
     * Updates the map settings for the given player. If no settings exist for the player,
     * it initializes new settings based on the player's current location. The map settings
     * are then marked for update.
     *
     * @param player the {@code Player} instance whose map settings need to be updated
     */
    public void updateStatus(Player player) {
        if (!mapHandler.getMapSettingsMap().containsKey(player.getUniqueId())) {
            mapHandler.getMapSettingsMap().put(player.getUniqueId(),
                    new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
        }

        mapHandler.getMapSettingsMap()
                .computeIfAbsent(player.getUniqueId(),
                        k -> new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()))
                .setUpdate(true);
    }

    /**
     * Creates and returns a new map item with default settings.
     * The map item is a filled map associated with a default map view.
     *
     * @return a new ItemStack representing the filled map with its metadata set
     */
    public ItemStack createMapItem() {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(Bukkit.getMap(0));
        mapItem.setItemMeta(meta);
        return mapItem;
    }

    /**
     * Retrieves the MapSettings for the given player. If no MapSettings exists for the player,
     * a new MapSettings instance is created, associated with the player, and returned.
     *
     * @param player the player whose MapSettings is being retrieved or created
     * @return the existing or newly created MapSettings associated with the player
     */
    public MapSettings getOrCreateMapSettings(Player player) {
        if (!mapHandler.getMapSettingsMap().containsKey(player.getUniqueId())) {
            mapHandler.getMapSettingsMap().put(player.getUniqueId(),
                    new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
        }

        return mapHandler.getMapSettingsMap()
                .computeIfAbsent(player.getUniqueId(),
                        k -> new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
    }

    /**
     * Determines the map color to represent the relationship between the player's clan
     * and another clan on a map.
     *
     * @param playerClan the clan of the player for whom the map is being generated
     * @param otherClan the other clan whose relationship to the player's clan is being evaluated
     * @return the corresponding map color representing the relationship between the clans
     */
    public MapColor getColourForClan(Clan playerClan, Clan otherClan) {
        ClanRelation clanRelation = clanManager.getRelation(playerClan, otherClan);
        MapColor materialColor = clanRelation.getMaterialColor();

        if (otherClan != null) {
            if (otherClan.isSafe()) {
                materialColor = MapColor.SNOW;
            } else if (otherClan.isAdmin()) {
                if (otherClan.getName().equalsIgnoreCase("Outskirts")) {
                    materialColor = MapColor.TERRACOTTA_PINK;
                } else {
                    materialColor = MapColor.COLOR_RED;
                }
            }
        }

        return materialColor;
    }
}