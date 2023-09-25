package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.events.*;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.nms.UtilMapMaterial;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@BPvPListener
public class MapListener implements Listener {


    private final Clans clans;
    private final MapHandler mapHandler;
    private final ClanManager clanManager;
    private final CooldownManager cooldownManager;
    private final GamerManager gamerManager;

    @Inject
    public MapListener(Clans clans, MapHandler mapHandler, ClanManager clanManager,
                       CooldownManager cooldownManager, GamerManager gamerManager) {
        this.clans = clans;
        this.mapHandler = mapHandler;
        this.clanManager = clanManager;
        this.cooldownManager = cooldownManager;
        this.gamerManager = gamerManager;

        mapHandler.loadMap();
    }

    //@EventHandler
    //public void onWorldLoad(WorldLoadEvent event) {
    //    if (event.getWorld().getName().equals("world")) {
    //        try {
    //            mapHandler.loadMap();
    //        } catch (Exception ex) {
    //            ex.printStackTrace();
    //        }
    //    }
    //}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        mapHandler.clanMapData.remove(player.getUniqueId());
        mapHandler.mapSettingsMap.remove(player.getUniqueId());
        for (ItemStack value : player.getInventory().all(Material.FILLED_MAP).values()) {
            player.getInventory().remove(value);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadChunks(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.FILLED_MAP);
    }

    @EventHandler
    public void onMapTransfer(InventoryClickEvent event) {
        if (event.getCurrentItem() == null)
            return;

        if (event.getCurrentItem().getType() == Material.FILLED_MAP) {
            final Inventory topInventory = event.getWhoClicked().getOpenInventory().getTopInventory();
            if (topInventory != null && topInventory.getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
            }
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEvent(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() == Material.FILLED_MAP) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEvent(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.FILLED_MAP) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanAlly(ClanRelationshipEvent event) {
        UtilServer.runTaskLater(clans, () -> {
            event.getClan().getMembers().forEach(this::updateClanChunks);
            event.getTargetClan().getMembers().forEach(this::updateClanChunks);
        }, 1);
    }

    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanEnemy(ClanEnemyEvent event) {
    //    updateClanRelation(event.getClan(), event.getOther());
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanTrust(ClanTrustEvent event) {
    //    updateClanRelation(event.getClan(), event.getOther());
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanNeutral(ClanNeutralEvent event) {
    //    updateClanRelation(event.getClan(), event.getOther());
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanRevokeTrust(ClanRevokeTrustEvent event) {
    //    updateClanRelation(event.getClan(), event.getOther());
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanPillageStart(ClanPillageStartEvent event) { updateClanRelation(event.getPillager(), event.getPillagee()); }
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanPillageEnd(ClanPillageEndEvent event) { updateClanRelation(event.getPillager(), event.getPillagee()); }
//
    ////TODO ADD PILLAGE EVENT
    //private void updateClanRelation(Clan clan, Clan other) {
    //    ClanManager.ClanRelation clanRelation = getManager(ClanManager.class).getClanRelation(clan, other);
    //    byte color;
    //    color = clanRelation.getMapColor();
    //    if (clan.getName().equals("Fields")) {
    //        color = 62;
    //    }
    //    if (clan.getName().equals("Red Shops") || clan.getName().equals("Red Spawn")) {
    //        color = 114;
    //    }
    //    if (clan.getName().equals("Blue Shops") || clan.getName().equals("Blue Spawn")) {
    //        color = (byte) 129;
    //    }
    //    if (clan.getName().equals("Outskirts")) {
    //        color = 74;
    //    }
    //    for (UUID uuid : other.getMemberMap().keySet()) {
    //        Player member = Bukkit.getPlayer(uuid);
    //        if (member == null) {
    //            continue;
    //        }
    //        if (!mapHandler.clanMapData.containsKey(uuid)) {
    //            mapHandler.clanMapData.put(uuid, new HashSet<>());
    //        }
    //        final Set<ChunkData> clanMapData = mapHandler.clanMapData.get(uuid);
    //        clanMapData.stream().filter(chunkData -> chunkData.getClan().equals(clan.getName())).forEach(chunkData -> chunkData.setColor(clanRelation.getMapColor()));
    //        if (clanMapData.stream().noneMatch(chunkData -> chunkData.getClan().equals(clan.getName()))) {
    //            for (String claim : clan.getClaims()) {
    //                final String[] split = claim.split(":");
    //                clanMapData.add(new ChunkData(split[0], color, Integer.parseInt(split[1]), Integer.parseInt(split[2]), clan.getName()));
    //            }
    //        }
    //        updateStatus(member);
    //    }
    //    for (UUID uuid : clan.getMemberMap().keySet()) {
    //        Player member = Bukkit.getPlayer(uuid);
    //        if (member == null) {
    //            continue;
    //        }
    //        if (!mapHandler.clanMapData.containsKey(uuid)) {
    //            mapHandler.clanMapData.put(uuid, new HashSet<>());
    //        }
//
    //        final Set<ChunkData> clanMapData = mapHandler.clanMapData.get(uuid);
    //        clanMapData.stream().filter(chunkData -> chunkData.getClan().equals(other.getName())).forEach(chunkData -> chunkData.setColor(clanRelation.getMapColor()));
    //        if (clanMapData.stream().noneMatch(chunkData -> chunkData.getClan().equals(other.getName()))) {
    //            for (String claim : other.getClaims()) {
    //                final String[] split = claim.split(":");
    //                clanMapData.add(new ChunkData(split[0], color, Integer.parseInt(split[1]), Integer.parseInt(split[2]), other.getName()));
    //            }
    //        }
    //        updateStatus(member);
    //    }
    //}
//


    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaim(ClanTerritoryEvent event) {
        if (event.isCancelled()) return;
        updateClaims(event.getClan());
    }

    private void updateClaims(Clan clan) {
        UtilServer.runTaskLater(clans, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {

                Clan otherClan = clanManager.getClanByPlayer(online).orElse(null);
                MapColor materialColor = getColourForClan(clan, otherClan);

                if (!mapHandler.clanMapData.containsKey(online.getUniqueId())) {
                    mapHandler.clanMapData.put(online.getUniqueId(), new HashSet<>());
                }

                mapHandler.clanMapData.get(online.getUniqueId()).removeIf(chunkData -> chunkData.getClan().equals(clan));
                for (ClanTerritory claim : clan.getTerritory()) {

                    Chunk chunk = UtilWorld.stringToChunk(claim.getChunk());
                    if (chunk != null) {
                        ChunkData chunkData = new ChunkData("world", materialColor, chunk.getX(), chunk.getZ(), clan);
                        for (int i = 0; i < 4; i++) {
                            BlockFace blockFace = BlockFace.values()[i];
                            Chunk targetChunk = online.getWorld().getChunkAt(chunk.getX() + blockFace.getModX(), chunk.getZ() + blockFace.getModZ());
                            Clan other = clanManager.getClanByChunk(targetChunk).orElse(null);
                            if (chunkData.getClan().equals(other)) {
                                chunkData.getBlockFaceSet().add(blockFace);
                            }
                        }

                        Set<ChunkData> chunkDataset = mapHandler.clanMapData.get(online.getUniqueId());
                        if (chunkDataset.stream().noneMatch(cd -> cd.equals(chunkData))) {
                            chunkDataset.add(chunkData);
                        }
                    }

                }
                updateStatus(online);
            }
        }, 1);
    }

    public void updateClanChunks(ClanMember member) {
        mapHandler.clanMapData.remove(UUID.fromString(member.getUuid()));
        Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
        if (player != null) {
            loadChunks(player);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanLeave(MemberLeaveClanEvent event) {
        if (event.isCancelled()) return;
        UtilServer.runTaskLater(clans, () -> {
            final Player player = event.getPlayer();
            if (!mapHandler.clanMapData.containsKey(player.getUniqueId())) {
                mapHandler.clanMapData.put(player.getUniqueId(), new HashSet<>());
            }
            for (ChunkData chunkData : mapHandler.clanMapData.get(player.getUniqueId())) {
                final IClan clan = chunkData.getClan();
                if (clan != null && !clan.isAdmin()) {
                    chunkData.setColor(UtilMapMaterial.getColorNeutral());
                }
            }
            updateStatus(player);
        }, 1);
    }

    //
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisband(ClanDisbandEvent event) {
        if (event.isCancelled()) return;
        UtilServer.runTaskLater(clans, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!mapHandler.clanMapData.containsKey(online.getUniqueId())) {
                    mapHandler.clanMapData.put(online.getUniqueId(), new HashSet<>());
                }
                mapHandler.clanMapData.get(online.getUniqueId()).removeIf(chunkData -> clanManager.getObject(chunkData.getClan().getName()).isEmpty());
                for (ChunkData chunkData : mapHandler.clanMapData.get(online.getUniqueId())) {
                    final IClan clan = chunkData.getClan();
                    if (clan != null && !clan.isAdmin()) {
                        ClanRelation relation = clanManager.getRelation(clan, clanManager.getClanByPlayer(online).orElse(null));
                        chunkData.setColor(relation.getMaterialColor());
                    }
                }
                updateStatus(online);
            }
        }, 1);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinClan(MemberJoinClanEvent event) {
        if (event.isCancelled()) return;
        UtilServer.runTaskLater(clans, () -> {
            final Player player = event.getPlayer();
            final IClan clan = event.getClan();

            if (!mapHandler.clanMapData.containsKey(player.getUniqueId())) {
                mapHandler.clanMapData.put(player.getUniqueId(), new HashSet<>());
            }

            Set<ChunkData> chunkData = mapHandler.clanMapData.get(player.getUniqueId());

            chunkData.forEach(cd -> {
                if (cd.getClan().equals(clan)) {
                    cd.setColor(ClanRelation.SELF.getMaterialColor());
                } else if (clan.isAllied(cd.getClan())) {
                    Optional<ClanAlliance> clanAllianceOptional = clan.getAlliance(cd.getClan());
                    clanAllianceOptional.ifPresent(clanAlliance -> {
                        cd.setColor(clanAlliance.isTrusted() ? ClanRelation.ALLY_TRUST.getMaterialColor() : ClanRelation.ALLY.getMaterialColor());
                    });
                } else if (clan.isEnemy(cd.getClan())) {
                    cd.setColor(ClanRelation.ENEMY.getMaterialColor());
                }
            });

            updateStatus(player);
        }, 1);

    }

    private void loadChunks(Player player) {
        if (!mapHandler.clanMapData.containsKey(player.getUniqueId())) {
            mapHandler.clanMapData.put(player.getUniqueId(), new HashSet<>());
        }

        Set<ChunkData> chunkClaimColor = mapHandler.clanMapData.get(player.getUniqueId());

        Clan pClan = clanManager.getClanByPlayer(player).orElse(null);

        for (Clan clan : clanManager.getObjects().values()) {

            MapColor materialColor = getColourForClan(pClan, clan);

            for (ClanTerritory claim : clan.getTerritory()) {
                Chunk chunk = UtilWorld.stringToChunk(claim.getChunk());
                if (chunk != null) {
                    ChunkData chunkData = new ChunkData("world", materialColor, chunk.getX(), chunk.getZ(), clan);
                    for (int i = 0; i < 4; i++) {
                        BlockFace blockFace = BlockFace.values()[i];
                        Chunk targetChunk = player.getWorld().getChunkAt(chunk.getX() + blockFace.getModX(), chunk.getZ() + blockFace.getModZ());
                        Clan other = clanManager.getClanByChunk(targetChunk).orElse(null);
                        if (chunkData.getClan().equals(other)) {
                            chunkData.getBlockFaceSet().add(blockFace);
                        }
                    }
                    chunkClaimColor.add(chunkData);
                }

            }

        }

        updateStatus(player);
    }

    private void updateStatus(Player player) {
        if (!mapHandler.mapSettingsMap.containsKey(player.getUniqueId())) {
            mapHandler.mapSettingsMap.put(player.getUniqueId(), new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
        }
        mapHandler.mapSettingsMap.get(player.getUniqueId()).setUpdate(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        final Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) {
            return;
        }
        if (!(event.getAction().name().contains("RIGHT") || event.getAction().name().contains("LEFT"))) {
            return;
        }
        if (!mapHandler.mapSettingsMap.containsKey(player.getUniqueId())) {
            mapHandler.mapSettingsMap.put(player.getUniqueId(), new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
        }
        final MapSettings mapSettings = mapHandler.mapSettingsMap.get(player.getUniqueId());

        if (!cooldownManager.add(player, "Map Zoom", 0.1, false, false)) {
            return;
        }

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        if (gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();

            if (event.getAction().name().contains("RIGHT")) {
                MapSettings.Scale curScale = mapSettings.getScale();

                if (curScale == MapSettings.Scale.NORMAL && !gamer.getClient().isAdministrating()) {
                    return;
                } else if (curScale == MapSettings.Scale.FAR) {
                    return;
                }

                UtilPlayer.sendActionBar(player, createZoomBar(mapSettings.setScale(MapSettings.Scale.values()[curScale.ordinal() + 1])));
                mapSettings.setUpdate(true);
            } else if (event.getAction().name().contains("LEFT")) {
                MapSettings.Scale curScale = mapSettings.getScale();

                if (curScale == MapSettings.Scale.CLOSEST) {
                    return;
                }
                UtilPlayer.sendActionBar(player, createZoomBar(mapSettings.setScale(MapSettings.Scale.values()[curScale.ordinal() - 1])));
                mapSettings.setUpdate(true);
            }

        }


    }

    private Component createZoomBar(MapSettings.Scale scale) {
        return Component.text("Zoom: ", NamedTextColor.WHITE)
                .append(Component.text((1 << scale.getValue()) + "x", NamedTextColor.GREEN));
    }

    private MapColor getColourForClan(Clan playerClan, Clan otherClan) {
        ClanRelation clanRelation = clanManager.getRelation(playerClan, otherClan);
        MapColor materialColor = clanRelation.getMaterialColor();

        if (otherClan.isSafe()) {
            materialColor = MapColor.SNOW;
        } else if (otherClan.isAdmin()) {
            if (otherClan.getName().equalsIgnoreCase("Outskirts")) {
                materialColor = MapColor.COLOR_ORANGE;
            } else {
                materialColor = MapColor.COLOR_RED;
            }
        }

        return materialColor;
    }
}
