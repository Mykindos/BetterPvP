package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

@CustomLog
@BPvPListener
public class ClansWorldListener extends ClanListener {

    @Inject
    @Config(path = "clans.claims.allow-gravity-blocks", defaultValue = "true")
    private boolean allowGravityBlocks;



    private final Clans clans;
    private final EffectManager effectManager;
    private final EnergyHandler energyHandler;

    @Inject
    public ClansWorldListener(ClanManager clanManager, ClientManager clientManager, Clans clans, EffectManager effectManager, EnergyHandler energyHandler) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.effectManager = effectManager;
        this.energyHandler = energyHandler;
    }

    @EventHandler
    public void onLogin(ClientJoinEvent event) {
        Optional<Clan> clanOptional = clanManager.getClanByClient(event.getClient());
        clanOptional.ifPresent(clan -> clan.setOnline(true));
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(event.getPlayer());

        UtilServer.runTaskLater(clans, () -> {
            clanOptional.ifPresent(clan -> {
                for (ClanMember member : clan.getMembers()) {
                    Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
                    if (player != null) {
                        return;
                    }
                }

                clan.setOnline(false);
            });
        }, 5L);

    }

    @UpdateEvent(delay = 1000)
    public void giveNoFallToPlayersInSpawn() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getY() < 150) continue;
            clanManager.getClanByLocation(player.getLocation()).ifPresent(clan -> {
                if (clan.isAdmin() && clan.getName().toLowerCase().contains("spawn")) {
                    effectManager.addEffect(player, EffectTypes.NO_FALL, 7000);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        final Client client = clientManager.search().online(player);
        Clan clan = clanManager.getClanByPlayer(player).orElse(null);

        if (client.isAdministrating()) {
            return;
        }

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(block.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (!locationClan.equals(clan)) {
                ClanRelation relation = clanManager.getRelation(clan, locationClan);

                // TODO this stuff

                //if (!(locationClan instanceof AdminClan)) {
                //    if (FarmBlocks.isCultivation(block.getType())) {
                //        return;
                //    }
                //}

                if (clanManager.getPillageHandler().isPillaging(clan, locationClan)) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.BREAK);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                        return;
                    }
                    clanManager.addInsurance(locationClan, block, InsuranceType.BREAK);
                    return;
                }

                final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.BREAK);
                tie.callEvent();
                if (tie.getResult() != Event.Result.DENY) {
                    return;
                }

                event.setCancelled(true);

                if (tie.isInform()) {
                    UtilMessage.simpleMessage(player, "Clans", "You cannot break <green>%s <gray>in %s<gray>.",
                            UtilFormat.cleanString(block.getType().name()),
                            relation.getPrimaryMiniColor() + "Clan " + locationClan.getName()
                    );
                }

            } else {
                if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER)) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.BREAK);
                    tie.callEvent();
                    if (tie.getResult() != Event.Result.DENY) {
                        return;
                    }

                    event.setCancelled(true);

                    if (tie.isInform()) {
                        UtilMessage.message(player, "Clans", "Clan Recruits cannot break blocks.");
                    }
                }
            }
        });

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        Block block = event.getBlock();

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(block.getLocation());

        if (client.isAdministrating()) {
            return;
        }

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }


        locationClanOptional.ifPresent(locationClan -> {

            ClanRelation relation = clanManager.getRelation(clan, locationClan);

            if (block.getType().hasGravity() && !allowGravityBlocks) {
                final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.PLACE);
                tie.callEvent();
                if (tie.getResult() != Event.Result.DENY) {
                    return;
                }

                event.setCancelled(true);

                if (tie.isInform()) {
                    UtilMessage.simpleMessage(player, "Clans", "You cannot place <green>%s <gray>in %s<gray>.",
                            UtilFormat.cleanString(block.getType().toString()), relation.getPrimaryMiniColor() + locationClan.getName());
                }
                return;
            }

            if (!locationClan.equals(clan)) {

                if (clanManager.getPillageHandler().isPillaging(clan, locationClan)) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.PLACE);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                        return;
                    }

                    clanManager.addInsurance(locationClan, block, InsuranceType.PLACE);
                    return;
                }

                final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.PLACE);
                tie.callEvent();
                if (tie.getResult() != Event.Result.DENY) {
                    return;
                }

                event.setCancelled(true);

                if (tie.isInform()) {
                    UtilMessage.simpleMessage(player, "Clans", "You cannot place <green>%s <gray>in %s<gray>.",
                            UtilFormat.cleanString(block.getType().name()),
                            relation.getPrimaryMiniColor() + "Clan " + locationClan.getName()
                    );
                }
            } else {
                if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER)) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.PLACE);
                    tie.callEvent();
                    if (tie.getResult() != Event.Result.DENY) {
                        return;
                    }

                    event.setCancelled(true);
                    if (tie.isInform()) {
                        UtilMessage.simpleMessage(player, "Clans", "Clan Recruits cannot place blocks.");
                    }
                }
            }
        });


    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) return;
        if (UtilBlock.isTutorial(block.getLocation())) return;

        Material material = block.getType();

        final Client client = clientManager.search().online(player);
        if (client.isAdministrating()) return;

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(block.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (locationClan != clan) {

                ClanRelation relation = clanManager.getRelation(clan, locationClan);

                if (locationClan.isAdmin() && material == Material.ENCHANTING_TABLE) return;
                if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) return;
                if (relation == ClanRelation.ALLY_TRUST && material.isInteractable()) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.INTERACT);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                    }
                    return;
                }

                if (clanManager.getPillageHandler().isPillaging(clan, locationClan)) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.INTERACT);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                    }
                    return;
                }

                if (UtilBlock.usable(block)) {

                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        if (material == Material.ENDER_CHEST) return;
                    }

                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.INTERACT);
                    tie.callEvent();
                    if (tie.getResult() != Event.Result.DENY) {
                        return;
                    }

                    event.setCancelled(true);

                    if (tie.isInform()) {
                        UtilMessage.simpleMessage(player, "Clans", "You cannot use <green>%s <gray>in %s<gray>.",
                                UtilFormat.cleanString(material.toString()),
                                relation.getPrimaryMiniColor() + "Clan " + locationClan.getName()
                        );
                    }
                }
            } else {
                if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER)) {
                    if (block.getState() instanceof Container) {
                        final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.INTERACT);
                        tie.callEvent();
                        if (tie.getResult() != Event.Result.DENY) {
                            return;
                        }

                        event.setCancelled(true);

                        if (tie.isInform()) {
                            UtilMessage.simpleMessage(player, "Clans", "Clan Recruits cannot access <green%s<gray>.",
                                    UtilFormat.cleanString(material.toString()));
                        }
                    }
                }
            }
        });

    }

    /*
     * Stops players from breaking other clans bases with pistons on the outside
     */
    @EventHandler
    public void onPistonEvent(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Optional<Clan> blockClanOptional = clanManager.getClanByLocation(block.getLocation());
            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());

            blockClanOptional.ifPresent(blockClan -> {
                if (!blockClanOptional.equals(locationClanOptional)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    /*
     * Stops players from breaking other clans bases with pistons on the outside
     */
    @EventHandler
    public void onPistonEvent(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Optional<Clan> blockClanOptional = clanManager.getClanByLocation(block.getLocation());
            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());

            blockClanOptional.ifPresent(blockClan -> {
                if (!blockClanOptional.equals(locationClanOptional)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    /*
     * Stops players from breaking Item Frames in admin territory
     */
    @EventHandler
    public void onBreak(HangingBreakByEntityEvent event) {
        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getEntity().getLocation());
        clanOptional.ifPresent(clan -> {
            if (!clan.isAdmin()) return;
            if (event.getRemover() instanceof Player player) {
                final Client client = clientManager.search().online(player);
                if (!client.isAdministrating()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    /*
     * Another method of stopping players from taking items or breaking Armour Stands
     */
    @EventHandler
    public void armorStand(PlayerArmorStandManipulateEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        if (!client.isAdministrating()) {
            event.setCancelled(true);
        }
    }

    /*
     * Stops Armour stands from being broken in admin territory
     */
    @EventHandler
    public void onArmorStandDeath(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand || event.getEntity() instanceof ItemFrame) {

            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getEntity().getLocation());
            clanOptional.ifPresent(clan -> {
                if (!clan.isAdmin()) return;
                if (event.getDamager() instanceof Player player) {
                    Client client = clientManager.search().online(player);
                    if (!client.isAdministrating()) {
                        event.setCancelled(true);
                    }
                }
            });

        }
    }

    /*
     * Stops players from interacting with item frames and armour stands (left click)
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material material = event.getClickedBlock().getType();
        if (material == Material.ITEM_FRAME || material == Material.ARMOR_STAND) {
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getClickedBlock().getLocation());
            clanOptional.ifPresent(clan -> {
                if (!clan.isAdmin()) return;
                Client client = clientManager.search().online(event.getPlayer());
                if (!client.isAdministrating()) {
                    event.setCancelled(true);
                }
            });

        }
    }

    /*
     * Stops players from taking stuff off armour stands and item frames in
     * admin territory
     */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof ItemFrame) {

            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getRightClicked().getLocation());
            clanOptional.ifPresent(clan -> {
                if (clan.isAdmin()) {
                    Client client = clientManager.search().online(event.getPlayer());
                    if (!client.isAdministrating()) {
                        event.setCancelled(true);
                    }
                }
            });

        }
    }

    /*
     * Logs the location and player of chests that are opened in the wilderness
     * Useful for catching xrayers.
     */
    @EventHandler
    public void onOpenChestInWilderness(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material m = event.getClickedBlock().getType();
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST
                || m == Material.FURNACE || m == Material.DROPPER || m == Material.CAULDRON
                || m == Material.SHULKER_BOX || m == Material.BARREL) {

            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getPlayer().getLocation());
            if (clanOptional.isPresent()) return;

            int x = (int) event.getClickedBlock().getLocation().getX();
            int y = (int) event.getClickedBlock().getLocation().getY();
            int z = (int) event.getClickedBlock().getLocation().getZ();
            log.info("{} opened a chest at {}, {}, {}, {}", event.getPlayer().getName(), event.getPlayer().getWorld().getName(), x, y, z);
        }

    }

    /**
     * Helps against people glitching through gates, maybe this isn't an issue in 1.19 anymore
     */
    @EventHandler
    public void onBreakGate(BlockBreakEvent event) {

        // TODO check if this is still necessary
        if (!event.getBlock().getType().name().contains("GATE")) return;
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());

        Gate gate = (Gate) event.getBlock().getState().getBlockData();
        if (gate.isOpen()) {
            locationClanOptional.ifPresent(locationClan -> {
                if (playerClanOptional.isEmpty() || !playerClanOptional.equals(locationClanOptional)) {
                    if (event.getPlayer().getLocation().distance(event.getBlock().getLocation()) < 1.5) {

                        Vector vec = UtilVelocity.getTrajectory(event.getPlayer().getLocation(), clanManager.closestWildernessBackwards(event.getPlayer()));
                        VelocityData velocityData = new VelocityData(vec, 0.5, true, 0.25, 0.25, 0.25, false);
                        UtilVelocity.velocity(event.getPlayer(), null, velocityData);

                    }
                }
            });
        }
    }

    /*
     * Turns lapis into water when placed.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLapisPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        if (event.getBlock().getType() == Material.LAPIS_BLOCK) {
            Client client = clientManager.search().online(event.getPlayer());
            if (client.isAdministrating()) return;

            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
            Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());

            if (locationClanOptional.isEmpty() || playerClanOptional.isEmpty() || !locationClanOptional.equals(playerClanOptional)) {
                if (event.getBlock().getLocation().getY() > 32) {
                    UtilMessage.message(event.getPlayer(), "Clans", "You can only place water in your own territory.");
                    event.setCancelled(true);
                    return;
                }
            }

            if (event.getBlock().getY() > 150) {
                UtilMessage.message(event.getPlayer(), "Clans", "You can only place water below 150Y");
                event.setCancelled(true);
                return;
            }
            Block block = event.getBlock();
            block.setType(Material.WATER);
            block.getLocation().getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
            block.getState().update();


        }
    }

    /*
     * Prevent obsidian from being broken by non admins
     */
    @EventHandler
    public void onBreakObsidian(BlockBreakEvent event) {

        if (event.getBlock().getType() == Material.OBSIDIAN) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
            if (clanOptional.isPresent()) {
                if (clanOptional.get().isAdmin()) {
                    UtilMessage.simpleMessage(player, "Server", "You cannot break <yellow>Obsidian<gray>.");
                    return;
                }
            }

            event.getBlock().setType(Material.AIR);
            UtilMessage.simpleMessage(player, "Server", "You cannot break <yellow>Obsidian<gray>.");
        }
    }

    /*
     * Stops leaf decay in admin clan territory
     */
    @EventHandler
    public void stopLeafDecay(LeavesDecayEvent event) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            event.setCancelled(true);
            return;
        }
        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
        clanOptional.ifPresent(clan -> {
            if (clan.isAdmin()) {
                event.setCancelled(true);
            }
        });

    }

    /*
     * Stops players from placing items such a levers and buttons on the outside of peoples bases
     * This is required, as previously, players could open the doors to an enemy base.
     */
    @EventHandler
    public void onAttachablePlace(BlockPlaceEvent event) {
        Material material = event.getBlock().getType();
        if (material == Material.LEVER || material.name().contains("_BUTTON") || material.name().contains("PRESSURE_PLATE")) {
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlockAgainst().getLocation());
            clanOptional.ifPresent(clan -> {
                Optional<Clan> playerClanOption = clanManager.getClanByPlayer(event.getPlayer());
                if (!playerClanOption.equals(clanOptional)) {
                    event.setCancelled(true);
                }
            });

        }
    }

    /**
     * Stop players shooting bows in safezones if they have not taken damage recently
     *
     * @param event The event
     */
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        clanManager.getClanByLocation(player.getLocation()).ifPresent(clan -> {
            if (clan.isSafe()) {
                final Gamer gamer = clientManager.search().online(player).getGamer();
                if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @UpdateEvent(delay = 250)
    public void checkGamemode() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            if (!player.getWorld().getName().equals("world")) {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.ADVENTURE);
                }
                continue;
            }

            Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
            if (locationClanOptional.isEmpty()) {
                if (player.getGameMode() == GameMode.ADVENTURE) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                continue;
            }

            Clan locationClan = locationClanOptional.get();

            if (locationClan.getName().equalsIgnoreCase("Fields")) {
                if (player.getGameMode() == GameMode.ADVENTURE) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                continue;
            }

            clanManager.getClanByPlayer(player).ifPresentOrElse(playerClan -> {
                if (locationClan.equals(playerClan)) {
                    if (player.getGameMode() == GameMode.ADVENTURE) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    return;
                }

                if (clanManager.getPillageHandler().isPillaging(playerClan, locationClan)) {
                    if (player.getGameMode() == GameMode.ADVENTURE) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    return;
                }

                // TODO check location clans farming levels

                player.setGameMode(GameMode.ADVENTURE);

            }, () -> player.setGameMode(GameMode.ADVENTURE));


        }
    }

    @EventHandler
    public void onFishMechanics(PlayerFishEvent event) {
        if (event.getCaught() instanceof Player player) {
            if (!energyHandler.use(event.getPlayer(), "Fishing Rod", 15.0, true)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            event.getHook().remove();

            if (player.equals(event.getPlayer())) return;

            if (clanManager.isInSafeZone(player)) {
                return;
            }

            if (player.getLocation().distance(event.getPlayer().getLocation()) < 2) {
                return;
            }

            var trajectory = UtilVelocity.getTrajectory(player, event.getPlayer()).normalize();
            player.setVelocity(trajectory.multiply(2).setY(Math.min(20, trajectory.getY())));

        }
    }

    @EventHandler
    public void onFishingHookHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook)) return;

        if (event.getHitEntity() instanceof Item) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFallingBlockSpawn(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
            if (clanOptional.isPresent()) {
                event.setCancelled(true);
            }
        }
    }
}
