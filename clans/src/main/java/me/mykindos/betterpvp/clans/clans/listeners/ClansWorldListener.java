package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.core.menu.CoreMenu;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import me.mykindos.betterpvp.core.world.events.PlayerUseStonecutterEvent;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

@CustomLog
@BPvPListener
@Singleton
public class ClansWorldListener extends ClanListener {

    private final Clans clans;
    private final EffectManager effectManager;
    private final EnergyHandler energyHandler;
    private final CooldownManager cooldownManager;
    private final WorldBlockHandler worldBlockHandler;
    private final ItemHandler itemHandler;

    @Inject
    @Config(path = "clans.claims.allow-gravity-blocks", defaultValue = "true")
    private boolean allowGravityBlocks;
    @Inject
    @Config(path = "clans.pillage.container-break-cooldown", defaultValue = "30.0")
    private double containerBreakCooldown;
    @Inject
    @Config(path = "clans.claims.allow-bubble-columns", defaultValue = "false")
    private boolean allowBubbleColumns;

    @Inject
    public ClansWorldListener(final ClanManager clanManager, final ClientManager clientManager, final Clans clans, final EffectManager effectManager, final EnergyHandler energyHandler, final CooldownManager cooldownManager, final WorldBlockHandler worldBlockHandler, ItemHandler itemHandler) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.effectManager = effectManager;
        this.energyHandler = energyHandler;
        this.cooldownManager = cooldownManager;
        this.worldBlockHandler = worldBlockHandler;
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onLogin(final PlayerJoinEvent event) {
        final Optional<Clan> clanOptional = this.clanManager.getClanByPlayer(event.getPlayer());
        clanOptional.ifPresent(clan -> clan.setOnline(true));
    }

    @EventHandler
    public void onLogout(final PlayerQuitEvent event) {
        final Optional<Clan> clanOptional = this.clanManager.getClanByPlayer(event.getPlayer());

        UtilServer.runTaskLater(this.clans, () -> {
            clanOptional.ifPresent(clan -> {
                for (final ClanMember member : clan.getMembers()) {
                    final Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
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
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getY() < 150) {
                continue;
            }
            this.clanManager.getClanByLocation(player.getLocation()).ifPresent(clan -> {
                if (clan.isAdmin() && clan.getName().toLowerCase().contains("spawn")) {
                    this.effectManager.addEffect(player, EffectTypes.NO_FALL, 7000);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        final Client client = this.clientManager.search().online(player);
        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);

        if (client.isAdministrating()) {
            return;
        }

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }

        final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(block.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (!locationClan.equals(clan)) {
                final ClanRelation relation = this.clanManager.getRelation(clan, locationClan);

                if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                    final TerritoryInteractEvent tie = UtilServer.callEvent(new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.BREAK));
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                        return;
                    }

                    if (block.getState() instanceof Container) {
                        if (!this.cooldownManager.use(player, "Break Container", this.containerBreakCooldown, true)) {
                            event.setCancelled(true);
                            return;
                        }
                    }

                    this.clanManager.addInsurance(locationClan, block, InsuranceType.BREAK);
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

    @EventHandler
    public void onInteractBed(final PlayerInteractEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand()) || !Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
            return;
        }

        final Block block = Objects.requireNonNull(event.getClickedBlock());
        final Optional<Clan> clanOpt = this.clanManager.getClanByChunk(block.getChunk());
        if (clanOpt.isEmpty() || !ClanCore.isCore(block)) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        final Clan clan = clanOpt.get();
        if (clan.getMemberByUUID(event.getPlayer().getUniqueId()).isEmpty()) {
            UtilMessage.message(event.getPlayer(), "Clans", "You cannot use this clan core.");
            return;
        }

        new CoreMenu(clan, event.getPlayer(), itemHandler).show(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Player player = event.getPlayer();
        final Client client = this.clientManager.search().online(player);
        final Block block = event.getBlock();

        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);
        final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(block.getLocation());

        if (client.isAdministrating()) {
            return;
        }

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }


        locationClanOptional.ifPresent(locationClan -> {

            final ClanRelation relation = this.clanManager.getRelation(clan, locationClan);

            if (block.getType().hasGravity() && !this.allowGravityBlocks) {
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

                if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.PLACE);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                        return;
                    }

                    this.clanManager.addInsurance(locationClan, block, InsuranceType.PLACE);
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (Event.Result.DENY.equals(event.useInteractedBlock())) {
            return;
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }
        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }

        final Material material = block.getType();

        if (event.getAction() == Action.PHYSICAL && material.equals(Material.TRIPWIRE)) {
            return;
        }

        final Client client = this.clientManager.search().online(player);
        if (client.isAdministrating()) {
            return;
        }

        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);
        final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(block.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (locationClan != clan) {

                final ClanRelation relation = this.clanManager.getRelation(clan, locationClan);

                if (locationClan.isAdmin() && material == Material.ENCHANTING_TABLE) {
                    return;
                }
                if (locationClan.isAdmin() && material == Material.CRAFTING_TABLE) {
                    return;
                }
                if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
                    return;
                }
                if (relation == ClanRelation.ALLY_TRUST && (block.getBlockData() instanceof Openable && block.getType() != Material.BARREL) && locationClan.isOnline()) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.INTERACT);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                    }
                    return;
                }

                if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DEFAULT, TerritoryInteractEvent.InteractionType.INTERACT);
                    tie.callEvent();
                    if (tie.getResult() == Event.Result.DENY) {
                        event.setCancelled(true);
                    }
                    return;
                }

                if (UtilBlock.usable(block)) {

                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        if (material == Material.ENDER_CHEST) {
                            return;
                        }
                    }

                    final TerritoryInteractEvent tie = new TerritoryInteractEvent(player, locationClan, block, Event.Result.DENY, TerritoryInteractEvent.InteractionType.INTERACT);
                    tie.callEvent();
                    if (tie.getResult() != Event.Result.DENY) {
                        return;
                    }

                    event.setCancelled(true);
                    event.setUseInteractedBlock(Event.Result.DENY);

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
                            UtilMessage.simpleMessage(player, "Clans", "Clan Recruits cannot access <green>%s<gray>.",
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
    public void onPlacePistonWilderness(final BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!event.getBlock().getType().name().contains("PISTON")) {
            return;
        }

        if (this.clanManager.getClanByLocation(event.getBlock().getLocation()).isEmpty()) {
            final Client client = this.clientManager.search().online(event.getPlayer());
            if (client.isAdministrating()) {
                return;
            }

            event.setCancelled(true);
            UtilMessage.simpleMessage(event.getPlayer(), "Restriction", "You cannot place pistons in the wilderness");
        }
    }

    /*
     * Stops players from breaking other clans bases with pistons on the outside
     */
    @EventHandler
    public void onPistonEvent(final BlockPistonRetractEvent event) {
        final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(event.getBlock().getLocation());

        for (final Block block : event.getBlocks()) {
            final Optional<Clan> blockClanOptional = this.clanManager.getClanByLocation(block.getLocation());
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
    public void onBreak(final HangingBreakByEntityEvent event) {
        final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getEntity().getLocation());
        clanOptional.ifPresent(clan -> {
            if (!clan.isAdmin()) {
                return;
            }
            if (event.getRemover() instanceof final Player player) {
                final Client client = this.clientManager.search().online(player);
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
    public void armorStand(final PlayerArmorStandManipulateEvent event) {
        final Client client = this.clientManager.search().online(event.getPlayer());
        if (!client.isAdministrating()) {
            event.setCancelled(true);
        }
    }

    /*
     * Stops Armour stands from being broken in admin territory
     */
    @EventHandler
    public void onArmorStandDeath(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand || event.getEntity() instanceof ItemFrame) {

            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getEntity().getLocation());
            clanOptional.ifPresent(clan -> {
                if (!clan.isAdmin()) {
                    return;
                }
                if (event.getDamager() instanceof final Player player) {
                    final Client client = this.clientManager.search().online(player);
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
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        final Material material = event.getClickedBlock().getType();
        if (material == Material.ITEM_FRAME || material == Material.ARMOR_STAND) {
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getClickedBlock().getLocation());
            clanOptional.ifPresent(clan -> {
                if (!clan.isAdmin()) {
                    return;
                }
                final Client client = this.clientManager.search().online(event.getPlayer());
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
    public void onInteract(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof ItemFrame) {

            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getRightClicked().getLocation());
            clanOptional.ifPresent(clan -> {
                if (clan.isAdmin()) {
                    final Client client = this.clientManager.search().online(event.getPlayer());
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
    public void onOpenChestInWilderness(final PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        final Material m = event.getClickedBlock().getType();
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST
                || m == Material.FURNACE || m == Material.DROPPER || m == Material.CAULDRON
                || m == Material.SHULKER_BOX || m == Material.BARREL) {

            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getPlayer().getLocation());
            if (clanOptional.isPresent()) {
                return;
            }

            final int x = (int) event.getClickedBlock().getLocation().getX();
            final int y = (int) event.getClickedBlock().getLocation().getY();
            final int z = (int) event.getClickedBlock().getLocation().getZ();
            log.info("{} opened a chest at {}, {}, {}, {}", event.getPlayer().getName(), event.getPlayer().getWorld().getName(), x, y, z).submit();
        }

    }

    /*
     * Turns lapis into water when placed.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLapisPlace(final BlockPlaceEvent event) {

        BPvPItem waterBlock = itemHandler.getItem("clans:water_block");
        if (waterBlock.matches(event.getItemInHand())) {
            final Client client = this.clientManager.search().online(event.getPlayer());
            if (client.isAdministrating()) {
                return;
            }

            final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(event.getBlock().getLocation());
            final Optional<Clan> playerClanOptional = this.clanManager.getClanByPlayer(event.getPlayer());

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
            final Block block = event.getBlock();
            block.setType(Material.WATER);
            block.getLocation().getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
            block.getState().update();


        }
    }

    /*
     * Prevent obsidian from being broken by non admins
     */
    @EventHandler
    public void onBreakObsidian(final BlockBreakEvent event) {

        if (event.getBlock().getType() == Material.OBSIDIAN) {
            final Player player = event.getPlayer();
            event.setCancelled(true);
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getBlock().getLocation());
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
    public void stopLeafDecay(final LeavesDecayEvent event) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            event.setCancelled(true);
            return;
        }
        final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getBlock().getLocation());
        clanOptional.ifPresent(clan -> {
            if (clan.isAdmin()) {
                event.setCancelled(true);
            }
        });

    }

    /**
     * Stop players shooting bows in safezones if they have not taken damage recently
     *
     * @param event The event
     */
    @EventHandler
    public void onShootBow(final EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        this.clanManager.getClanByLocation(player.getLocation()).ifPresent(clan -> {
            if (clan.isSafe()) {
                final Gamer gamer = this.clientManager.search().online(player).getGamer();
                if (!gamer.isInCombat()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkClaim(final ChunkClaimEvent event) {
        event.getChunk().getPersistentDataContainer().set(ClansNamespacedKeys.CLAN,
                CustomDataType.UUID,
                event.getClan().getId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnclaim(final ChunkUnclaimEvent event) {
        event.getChunk().getPersistentDataContainer().remove(ClansNamespacedKeys.CLAN);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        this.clanManager.expensiveGetClanByPlayer(event.getPlayer()).ifPresentOrElse(clan -> {
            event.getPlayer().setMetadata("clan", new FixedMetadataValue(this.clans, clan.getId()));
            clan.getMember(event.getPlayer().getUniqueId()).setClientName(event.getPlayer().getName());
        }, () -> {
            event.getPlayer().removeMetadata("clan", this.clans);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        event.getPlayer().removeMetadata("clan", this.clans);
    }

    @UpdateEvent(delay = 250)
    public void checkGamemode() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE
                    || player.getGameMode() == GameMode.SPECTATOR
                    || this.effectManager.hasEffect(player, EffectTypes.FROZEN)) {
                continue;
            }

            if (!player.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) {
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.ADVENTURE);
                }
                continue;
            }

            final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(player.getLocation());
            if (locationClanOptional.isEmpty()) {
                if (player.getGameMode() == GameMode.ADVENTURE) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                continue;
            }

            final Clan locationClan = locationClanOptional.get();

            if (locationClan.getName().equalsIgnoreCase("Fields")) {
                if (player.getGameMode() == GameMode.ADVENTURE) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                continue;
            }

            this.clanManager.getClanByPlayer(player).ifPresentOrElse(playerClan -> {
                if (locationClan.equals(playerClan)) {
                    if (player.getGameMode() == GameMode.ADVENTURE) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    return;
                }

                if (this.clanManager.getPillageHandler().isPillaging(playerClan, locationClan)) {
                    if (player.getGameMode() == GameMode.ADVENTURE) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    return;
                }

                player.setGameMode(GameMode.ADVENTURE);

            }, () -> player.setGameMode(GameMode.ADVENTURE));


        }
    }

    @EventHandler
    public void onFishMechanics(final PlayerFishEvent event) {

        if (event.getCaught() instanceof final Player player) {
            if (!this.energyHandler.use(event.getPlayer(), "Fishing Rod", 15.0, true)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            event.getHook().remove();

            if (player.equals(event.getPlayer())) {
                return;
            }

            Optional<Clan> playerClanoptional = this.clanManager.getClanByPlayer(event.getPlayer());
            if (playerClanoptional.isEmpty()) {
                return;
            }

            Optional<Clan> targetClanOptional = this.clanManager.getClanByPlayer(player);
            if (targetClanOptional.isEmpty()) {
                return;
            }

            Clan targetClan = targetClanOptional.get();
            Clan playerClan = playerClanoptional.get();

            if (!targetClan.equals(playerClan) && !targetClan.isAllied(playerClan)) {
                return;
            }

            if (effectManager.hasEffect(player, EffectTypes.PROTECTION)) {
                return;
            }

            if (this.clanManager.isInSafeZone(player)) {
                return;
            }

            if (player.getLocation().distance(event.getPlayer().getLocation()) < 2) {
                return;
            }

            final var trajectory = UtilVelocity.getTrajectory(player, event.getPlayer()).normalize();
            player.setVelocity(trajectory.multiply(2).setY(Math.min(20, trajectory.getY())));

        }
    }

    @EventHandler
    public void onFishingHookHit(final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook)) {
            return;
        }

        if (event.getHitEntity() instanceof Item) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFallingBlockSpawn(final EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getBlock().getLocation());
            if (clanOptional.isPresent()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBedDrop(final ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.RED_BED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobSpawnInClaim(final CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(event.getLocation());
            if (clanOptional.isPresent()) {
                event.setCancelled(true);
            }
        } else if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMoveBubbleColumn(final PlayerMoveEvent event) {
        if (this.allowBubbleColumns) {
            return;
        }

        final Block block = event.getPlayer().getLocation().getBlock();
        if (block.getType() != Material.BUBBLE_COLUMN) {
            return;
        }
        final Clan clan = this.clanManager.getClanByLocation(event.getPlayer().getLocation()).orElse(null);
        if (clan == null || clan.isAdmin()) {
            return;
        }

        final Clan playerClan = this.clanManager.getClanByPlayer(event.getPlayer()).orElse(null);
        if ((playerClan != null && !clan.equals(playerClan))) {
            for (int i = 0; i < 100; i++) {
                final Block newBlock = block.getLocation().add(0, block.getY() - i, 0).getBlock();
                if (newBlock.getType() == Material.SOUL_SAND || newBlock.getType() == Material.MAGMA_BLOCK) {
                    this.worldBlockHandler.addRestoreBlock(newBlock, Material.STONE, 15_000);
                    break;
                }
            }

        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilUse(PrepareAnvilEvent event) {
        event.getView().setRepairCost(9001);
    }

    @EventHandler
    public void onWaterPlace(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (!event.getItem().getType().equals(Material.WATER_BUCKET)) return;
        final Client client = this.clientManager.search().online(event.getPlayer());
        if (client.isAdministrating()) return;

        event.setUseItemInHand(Event.Result.DENY);
        event.getPlayer().getInventory().remove(Material.WATER_BUCKET);
        UtilMessage.message(event.getPlayer(), "Clans", "Your <yellow>Bucket</yellow> broke!");
    }

    private final ConcurrentLinkedQueue<Clan> clanPdcQueue = new ConcurrentLinkedQueue<>();

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (event.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {

            clanPdcQueue.addAll(clanManager.getObjects().values());

        }
    }

    @UpdateEvent(delay = 100)
    public void updateChunkPdcSlowly() {
        if (clanPdcQueue.isEmpty()) return;
        if (Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME) == null) return;

        Clan clan = clanPdcQueue.poll();
        if (clan == null || clan.getTerritory().isEmpty()) return;

        ClanCore core = clan.getCore();
        if (core.getPosition() != null) {
            core.removeBlock();
            core.placeBlock();
        }

        clan.getTerritory().forEach(clanTerritory -> {
            Chunk chunk = clanTerritory.getWorldChunk();
            chunk.getPersistentDataContainer().set(ClansNamespacedKeys.CLAN, CustomDataType.UUID, clan.getId());
        });
    }

    @EventHandler
    public void onCoreExplode(BlockExplodeEvent event) {
        if (event.getBlock().getType() == Material.RESPAWN_ANCHOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRedstoneItemPlace(BlockPlaceEvent event) {

        Block block = event.getBlockPlaced();

        if (UtilBlock.isRedstone(block)) {

            // Don't run the code if the block was placed within a claim
            if (clanManager.getClanByLocation(block.getLocation()).isPresent()) {
                return;
            }

            final int LOWER_BOUND = -1;
            Player player = event.getPlayer();
            Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

            for (int x = LOWER_BOUND; x <= 1; x++) {
                for (int z = LOWER_BOUND; z <= 1; z++) {
                    Block targetBlock = event.getBlockPlaced().getRelative(x, 0, z);

                    Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                    if (targetBlockLocationClanOptional.isPresent()) {
                        if (playerClan == null || !playerClan.equals(targetBlockLocationClanOptional.get())) {
                            UtilMessage.message(player, "Clans", "You cannot place this block on the edge of a claim.");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;

        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
        if (clanOptional.isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;

        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
        if (clanOptional.isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleOreReplacements(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Clan clan = clanManager.getClanByLocation(event.getBlock().getLocation()).orElse(null);
        if (clan == null || !clan.isAdmin()) {
            if (clan != null) {
                Clan playerClan = clanManager.getClanByPlayer(event.getPlayer()).orElse(null);
                if (clan != playerClan) {
                    return;
                }
            }
            Block block = event.getBlock();
            if (block.getType() == Material.COPPER_ORE || block.getType() == Material.DEEPSLATE_COPPER_ORE) {
                event.setDropItems(false);
                Item item = block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.LEATHER, 1));
                if (effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) {
                    UtilItem.reserveItem(item, event.getPlayer(), 10.0);
                }

            } else if (block.getType() == Material.GILDED_BLACKSTONE) {
                event.setDropItems(false);
                Item item = block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.NETHERITE_INGOT, 1));
                if (effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) {
                    UtilItem.reserveItem(item, event.getPlayer(), 10.0);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMinecartCollide(VehicleEntityCollisionEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        if (playerClanOptional.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getVehicle().getLocation());
        if (locationClanOptional.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        if (playerClanOptional.get().equals(locationClanOptional.get())) return;

        event.setCancelled(true);

    }

    @EventHandler
    public void onBeeSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.BEE) {
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
                event.setCancelled(true);
                return;
            } else if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BEEHIVE) {
                Bee bee = (Bee) event.getEntity();
                bee.setCustomNameVisible(false);
                bee.customName(Component.text("Bee", NamedTextColor.YELLOW));
                bee.setRemoveWhenFarAway(false);
                bee.setPersistent(true);
                Objects.requireNonNull(bee.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(50.0);
                bee.setHealth(50.0);
            }
        }
    }

    @EventHandler
    public void onSnowGolemSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreakBeeNest(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BEE_NEST) {

            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler
    public void onBeeNestSpawn(StructureGrowEvent event) {
        event.getBlocks().removeIf(block -> block.getType() == Material.BEE_NEST);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        if (event.getEntity() instanceof Player) return;

        if (UtilBlock.isPressurePlate(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerStepOnPlate(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        if (!UtilBlock.isPressurePlate(clickedBlock)) return;
        final Optional<Clan> clanOptional = clanManager.getClanByLocation(clickedBlock.getLocation());

        if (clanOptional.isEmpty()) return;

        final Clan clan = clanOptional.get();

        if (clan.isAdmin()) return;
        if (clan.isOnline()) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onSalvage(PlayerUseStonecutterEvent event) {
        Material material = event.getItem().getType();
        if (material == Material.DIAMOND_AXE || material == Material.GOLDEN_AXE
                || material == Material.DIAMOND_SWORD || material == Material.GOLDEN_SWORD) {
            event.cancel("Cannot salvage this item");
        }
    }

    @EventHandler
    public void onThrowSnowballOrEgg(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack itemInMainHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInMainHand.getType() == Material.SNOWBALL || itemInMainHand.getType() == Material.EGG) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.getPlayer().getInventory().remove(itemInMainHand);
            event.setCancelled(true);
        }
    }

}
