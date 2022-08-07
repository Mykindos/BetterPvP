package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.components.ClanTerritory;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.minecraft.world.level.material.MaterialColor;
import org.bukkit.*;
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
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

import java.util.HashSet;
import java.util.Set;

@BPvPListener
public class MapListener implements Listener {


    private final MapHandler mapHandler;
    private final ClanManager clanManager;
    private final CooldownManager cooldownManager;

    @Inject
    public MapListener(MapHandler mapHandler, ClanManager clanManager, CooldownManager cooldownManager) {
        this.mapHandler = mapHandler;
        this.clanManager = clanManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (event.getWorld().getName().equals("world")) {
            System.out.println("Loading map");
            try {
                mapHandler.loadMap();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        mapHandler.clanMapData.remove(player.getUniqueId());
        mapHandler.mapSettingsMap.remove(player.getUniqueId());
        for (ItemStack value : player.getInventory().all(Material.MAP).values()) {
            player.getInventory().remove(value);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadChunks(event.getPlayer());

        ItemStack is = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) is.getItemMeta();
        meta.setMapView(Bukkit.getMap(0));
        is.setItemMeta( meta );
        event.getPlayer().getInventory().addItem(is);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.MAP);
    }

    @EventHandler
    public void onMapTransfer(InventoryClickEvent event) {
        if (event.getCurrentItem() == null)
            return;

        if (event.getCurrentItem().getType() == Material.MAP) {
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
            if (item != null && item.getType() == Material.MAP) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEvent(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item != null && item.getType() == Material.MAP) {
            event.getItemDrop().remove();
        }
    }

    // TODO
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanAlly(ClanAllyEvent event) {
    //    updateClanRelation(event.getClan(), event.getOther());
    //}
//
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
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onUnclaim(ClanUnclaimEvent event) {
    //    updateClaims(event.getClan());
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClaim(ClanClaimEvent event) {
    //    updateClaims(event.getClan());
    //}
//
    //private void updateClaims(Clan clan) {
    //    for (Player online : Bukkit.getOnlinePlayers()) {
    //        final ClanManager.ClanRelation clanRelation = getManager(ClanManager.class).getClanRelation(clan, getManager(ClanManager.class).getClan(online));
    //        byte color;
    //        color = clanRelation.getMapColor();
    //        if (clan.getName().equals("Fields")) {
    //            color = 62;
    //        }
    //        if (clan.getName().equals("Red Shops") || clan.getName().equals("Red Spawn")) {
    //            color = 114;
    //        }
    //        if (clan.getName().equals("Blue Shops") || clan.getName().equals("Blue Spawn")) {
    //            color = (byte) 129;
    //        }
    //        if (clan.getName().equals("Outskirts")) {
    //            color = 74;
    //        }
    //        if (!mapHandler.clanMapData.containsKey(online.getUniqueId())) {
    //            mapHandler.clanMapData.put(online.getUniqueId(), new HashSet<>());
    //        }
    //        mapHandler.clanMapData.get(online.getUniqueId()).removeIf(chunkData -> getManager(ClanManager.class).getClan(online.getWorld().getName(), chunkData.getX(), chunkData.getZ()) == null);
    //        for (String claim : clan.getClaims()) {
    //            final String[] split = claim.split(":");
    //            final String world = split[0];
    //            final int x = Integer.parseInt(split[1]);
    //            final int z = Integer.parseInt(split[2]);
    //            if (mapHandler.clanMapData.get(online.getUniqueId()).stream().noneMatch(chunkData -> chunkData.getX() == x && chunkData.getZ() == z && chunkData.getClan().equals(clan.getName()))) {
    //                final ChunkData e = new ChunkData(world, color, x, z, clan.getName());
    //                mapHandler.clanMapData.get(online.getUniqueId()).add(e);
    //            }
    //        }
    //        for (ChunkData chunkData : mapHandler.clanMapData.get(online.getUniqueId())) {
    //            chunkData.getBlockFaceSet().clear();
    //            for (int i = 0; i < 4; i++) {
    //                BlockFace blockFace = BlockFace.values()[i];
    //                final Clan other = getManager(MapManager.class).getManager(ClanManager.class).getClan(online.getWorld().getName(), chunkData.getX() + blockFace.getModX(), chunkData.getZ() + blockFace.getModZ());
    //                if (other != null && chunkData.getClan().equals(other.getName())) {
    //                    chunkData.getBlockFaceSet().add(blockFace);
    //                }
    //            }
    //        }
    //        updateStatus(online);
    //    }
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onClanLeave(ClanLeaveEvent event) {
    //    final Player player = event.getPlayer();
    //    if (!mapHandler.clanMapData.containsKey(player.getUniqueId())) {
    //        mapHandler.clanMapData.put(player.getUniqueId(), new HashSet<>());
    //    }
    //    for (ChunkData chunkData : mapHandler.clanMapData.get(player.getUniqueId())) {
    //        final Clan clan = getManager(ClanManager.class).getClan(chunkData.getClan());
    //        if (clan != null && !clan.isAdmin()) {
    //            chunkData.setColor(ClanManager.ClanRelation.NEUTRAL.getMapColor());
    //        }
    //    }
    //    updateStatus(player);
    //}
//
    //@EventHandler(priority = EventPriority.LOWEST)
    //public void onDisband(ClanDisbandEvent event) {
    //    for (Player online : Bukkit.getOnlinePlayers()) {
    //        if (!mapHandler.clanMapData.containsKey(online.getUniqueId())) {
    //            mapHandler.clanMapData.put(online.getUniqueId(), new HashSet<>());
    //        }
    //        mapHandler.clanMapData.get(online.getUniqueId()).removeIf(chunkData -> getManager(ClanManager.class).getClan(chunkData.getClan()) == null);
    //        for (ChunkData chunkData : mapHandler.clanMapData.get(online.getUniqueId())) {
    //            final Clan clan = getManager(ClanManager.class).getClan(chunkData.getClan());
    //            if (clan != null && !clan.isAdmin()) {
    //                chunkData.setColor(getManager(ClanManager.class).getClanRelation(clan, getManager(ClanManager.class).getClan(online.getUniqueId())).getMapColor());
    //            }
    //        }
    //        updateStatus(online);
    //    }
    //}
//
    //@EventHandler(priority = EventPriority.MONITOR)
    //public void onPlayerJoinClan(ClanJoinEvent event) {
    //    final Player player = event.getPlayer();
    //    final Clan clan = getManager(ClanManager.class).getClan(player);
    //    if (clan == null) {
    //        return;
    //    }
    //    if (!mapHandler.clanMapData.containsKey(player.getUniqueId())) {
    //        mapHandler.clanMapData.put(player.getUniqueId(), new HashSet<>());
    //    }
    //    for (String claim : clan.getClaims()) {
    //        final int x = Integer.parseInt(claim.split(":")[1]);
    //        final int z = Integer.parseInt(claim.split(":")[2]);
    //        mapHandler.clanMapData.get(player.getUniqueId()).stream().filter(chunkData -> chunkData.getX() == x && chunkData.getZ() == z && chunkData.getClan().equals(clan.getName())).forEach(chunkData -> chunkData.setColor(ClanManager.ClanRelation.SELF.getMapColor()));
    //    }
    //    for (String ally : clan.getAllianceMap().keySet()) {
    //        Clan allyClan = getManager(ClanManager.class).getClan(ally);
    //        ClanManager.ClanRelation clanRelation = getManager(ClanManager.class).getClanRelation(clan, allyClan);
    //        for (String claim : allyClan.getClaims()) {
    //            final int x = Integer.parseInt(claim.split(":")[1]);
    //            final int z = Integer.parseInt(claim.split(":")[2]);
    //            mapHandler.clanMapData.get(player.getUniqueId()).stream().filter(chunkData -> chunkData.getX() == x && chunkData.getZ() == z && chunkData.getClan().equals(ally)).forEach(chunkData -> chunkData.setColor(clanRelation.getMapColor()));
    //        }
    //    }
    //    for (String enemy : clan.getAllianceMap().keySet()) {
    //        Clan enemyClan = getManager(ClanManager.class).getClan(enemy);
    //        ClanManager.ClanRelation clanRelation = getManager(ClanManager.class).getClanRelation(clan, enemyClan);
    //        for (String claim : enemyClan.getClaims()) {
    //            final int x = Integer.parseInt(claim.split(":")[1]);
    //            final int z = Integer.parseInt(claim.split(":")[2]);
    //            mapHandler.clanMapData.get(player.getUniqueId()).stream().filter(chunkData -> chunkData.getX() == x && chunkData.getZ() == z && chunkData.getClan().equals(enemy)).forEach(chunkData -> chunkData.setColor(clanRelation.getMapColor()));
    //        }
    //    }
    //    updateStatus(player);
    //}

    private void loadChunks(Player player) {
        if (!mapHandler.clanMapData.containsKey(player.getUniqueId())) {
            mapHandler.clanMapData.put(player.getUniqueId(), new HashSet<>());
        }

        final Set<ChunkData> chunkClaimColor = mapHandler.clanMapData.get(player.getUniqueId());


        Clan pClan = clanManager.getClanByPlayer(player).orElse(null);

        for (Clan clan : clanManager.getObjects().values()) {
            ClanRelation clanRelation = clanManager.getRelation(pClan, clan);

            MaterialColor materialColor = clanRelation.getMaterialColor();

            for (ClanTerritory claim : clan.getTerritory()) {

                Chunk chunk = UtilWorld.stringToChunk(claim.getChunk());
                if (chunk != null) {
                    ChunkData chunkData = new ChunkData("world", materialColor, chunk.getX(), chunk.getZ(), clan.getName());
                    for (int i = 0; i < 4; i++) {
                        BlockFace blockFace = BlockFace.values()[i];
                        Chunk targetChunk = player.getWorld().getChunkAt(chunk.getX() + blockFace.getModX(), chunk.getZ() + blockFace.getModZ());
                        Clan other = clanManager.getClanByChunk(targetChunk).orElse(null);
                        if (other != null && chunkData.getClan().equals(other.getName())) {
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
        if (player.getInventory().getItemInMainHand().getType() != Material.MAP) {
            return;
        }
        if (!(event.getAction().name().contains("RIGHT") || event.getAction().name().contains("LEFT"))) {
            return;
        }
        if (!mapHandler.mapSettingsMap.containsKey(player.getUniqueId())) {
            mapHandler.mapSettingsMap.put(player.getUniqueId(), new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
        }
        final MapSettings mapSettings = mapHandler.mapSettingsMap.get(player.getUniqueId());

        if (!cooldownManager.add(player, "Map Zoom", 2.5, false, false)) {
            return;
        }

        if (event.getAction().name().contains("RIGHT")) {
            MapSettings.Scale curScale = mapSettings.getScale();

            if (curScale == MapSettings.Scale.FAR) {
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

    private String createZoomBar(MapSettings.Scale scale) {
        return ChatColor.WHITE + "Zoom Factor: " + ChatColor.GREEN + (1 << scale.getValue()) + "x";
    }
}
