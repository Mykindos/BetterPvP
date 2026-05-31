package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.core.menu.CoreMenu;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.store.events.PlayerMountEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.ZoneInteractEvent;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Leaves;
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
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

@CustomLog
@BPvPListener
@Singleton
public class ClansWorldListener extends ClanListener {

    private final Clans clans;
    private final EffectManager effectManager;
    private final EnergyService energyService;
    private final CooldownManager cooldownManager;
    private final WorldBlockHandler worldBlockHandler;
    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final BlockTagManager blockTagHandler;
    private final ZoneManager zoneManager;
    public static final String AGGRESSIVE_RODDER_UNLOCKED = "aggressive_rodder_unlocked";

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
    public ClansWorldListener(final ClanManager clanManager, final ClientManager clientManager, final Clans clans,
                              final EffectManager effectManager, final EnergyService energyService, final CooldownManager cooldownManager,
                              final WorldBlockHandler worldBlockHandler, ItemRegistry itemRegistry, ItemFactory itemFactory, BlockTagManager blockTagHandler,
                              ZoneManager zoneManager) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.effectManager = effectManager;
        this.energyService = energyService;
        this.cooldownManager = cooldownManager;
        this.worldBlockHandler = worldBlockHandler;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.blockTagHandler = blockTagHandler;
        this.zoneManager = zoneManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupArmour(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            ItemStack itemStack = event.getItem().getItemStack();
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof ArmorMeta armorMeta) {
                ArmorTrim trim = armorMeta.getTrim();
                if (trim != null) {

                    final String material = String.valueOf(trim.getMaterial());

                    if (material.contains("holder=Direct")) {
                        armorMeta.setTrim(new ArmorTrim(TrimMaterial.LAPIS, TrimPattern.DUNE));
                        log.warn("Modified armor trim for player: " + event.getEntity().getName()).submit();
                    }

                    itemStack.setItemMeta(armorMeta);
                }
            }

        }
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
                    final Player player = Bukkit.getPlayer(member.getUuid());
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
            if (this.zoneManager.hasTagAt(player.getLocation(), Zones.SAFE)) {
                this.effectManager.addEffect(player, EffectTypes.NO_FALL, 7000);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (event.getBlock().getType() == Material.FARMLAND && event.getTo() == Material.DIRT) {
            event.setCancelled(true);
        }
    }

    /**
     * Access gate for {@link ScriptedBlockPlaceEvent}. Runs at NORMAL — earlier listeners
     * (e.g. {@code FieldsListener} at LOW) get a chance to set
     * {@link Event.Result#ALLOW} and bypass the check. If nobody has claimed the placement
     * (result still {@link Event.Result#DEFAULT}) and the location is in another clan's
     * territory, we silently cancel via a {@code zoneManager.queryAccess} probe with
     * {@code inform=false} — no chat spam for ability-driven placements.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onScriptedBlockPlace(final ScriptedBlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (event.getResult() != Event.Result.DEFAULT) return;

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (UtilBlock.isTutorial(block.getLocation())) return;

        if (clanManager.getClanByLocation(block.getLocation()).isEmpty()) return; // unclaimed — allow

        if (zoneManager.queryAccess(player, block.getLocation(), ZoneInteraction.PLACE, block, false) == Event.Result.DENY) {
            event.setCancelled(true);
        }
    }

    /**
     * Territory protection consumes the centralized {@link ZoneInteractEvent} (fired by the core
     * {@code ZoneInteractionListener}) rather than hooking the Bukkit block events directly.
     * <p>
     * This handler is the <b>decision</b>: it runs the relation/rank/pillage logic and sets the verdict to
     * {@code DENY} when the action is disallowed — the core listener applies the cancellation. It runs at {@code LOW}
     * so later consumers (e.g. the Fields ore system at {@code HIGHEST}) see the verdict and can react. The denial
     * <b>message</b> is emitted separately at {@code MONITOR} ({@link #onTerritoryDenyMessage}) so consumers that
     * suppress it (Fields, mining detonation) are honoured.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onTerritoryZoneInteract(final ZoneInteractEvent event) {
        if (!event.getZone().hasTag(ClanZones.TERRITORY)) {
            return;
        }

        final Block block = event.getBlock();
        if (block == null) {
            return;
        }

        final Player player = event.getPlayer();
        final Client client = this.clientManager.search().online(player);
        if (client.isAdministrating()) {
            return;
        }

        if (UtilBlock.isTutorial(block.getLocation())) {
            return;
        }

        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);
        final Clan locationClan = this.clanManager.getClanByLocation(block.getLocation()).orElse(null);
        if (locationClan == null) {
            return;
        }

        switch (event.getInteraction()) {
            case BREAK -> handleTerritoryBreak(event, player, clan, locationClan, block);
            case PLACE -> handleTerritoryPlace(event, player, clan, locationClan, block);
            case INTERACT -> handleTerritoryInteract(event, player, clan, locationClan, block);
            default -> {
            }
        }
    }

    private void handleTerritoryBreak(ZoneInteractEvent event, Player player, Clan clan, Clan locationClan, Block block) {
        if (!locationClan.equals(clan)) {
            if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                if (block.getState() instanceof Container
                        && !this.cooldownManager.use(player, "Break Container", this.containerBreakCooldown, true)) {
                    event.setResult(Event.Result.DENY);
                    return;
                }

                this.clanManager.addInsurance(locationClan, block, InsuranceType.BREAK);
                return;
            }

            event.setResult(Event.Result.DENY);
        } else {
            if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    private void handleTerritoryPlace(ZoneInteractEvent event, Player player, Clan clan, Clan locationClan, Block block) {
        if (block.getType().hasGravity() && !this.allowGravityBlocks) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (!locationClan.equals(clan)) {
            if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                this.clanManager.addInsurance(locationClan, block, InsuranceType.PLACE);
                return;
            }

            event.setResult(Event.Result.DENY);
        } else {
            if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    private void handleTerritoryInteract(ZoneInteractEvent event, Player player, Clan clan, Clan locationClan, Block block) {
        final Material material = block.getType();

        if (locationClan != clan) {
            if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
                return;
            }

            final ClanRelation relation = this.clanManager.getRelation(clan, locationClan);
            if (relation == ClanRelation.ALLY_TRUST && block.getBlockData() instanceof Openable && material != Material.BARREL && locationClan.isOnline()) {
                return;
            }

            if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                return;
            }

            if (UtilBlock.usable(block)) {
                event.setResult(Event.Result.DENY);
            }
        } else {
            if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.MEMBER) && block.getState() instanceof Container) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    /**
     * Emits the territory denial message at {@code MONITOR}, after all consumers have finalized the verdict and the
     * {@code inform} flag — so a consumer that handled the interaction another way (Fields ore mining, mining
     * detonation) can suppress the message by clearing {@code inform}.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTerritoryDenyMessage(final ZoneInteractEvent event) {
        if (event.getResult() != Event.Result.DENY || !event.isInform()) {
            return;
        }
        if (!event.getZone().hasTag(ClanZones.TERRITORY)) {
            return;
        }

        final Block block = event.getBlock();
        if (block == null) {
            return;
        }

        final Player player = event.getPlayer();
        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);
        final Clan locationClan = this.clanManager.getClanByLocation(block.getLocation()).orElse(null);
        if (locationClan == null) {
            return;
        }

        final String blockName = UtilFormat.cleanString(block.getType().name());
        if (!locationClan.equals(clan)) {
            final ClanRelation relation = this.clanManager.getRelation(clan, locationClan);
            final String owner = relation.getPrimaryMiniColor() + "Clan " + locationClan.getName();
            switch (event.getInteraction()) {
                case BREAK -> UtilMessage.simpleMessage(player, "Clans", "You cannot break <green>%s <gray>in %s<gray>.", blockName, owner);
                case PLACE -> UtilMessage.simpleMessage(player, "Clans", "You cannot place <green>%s <gray>in %s<gray>.", blockName, owner);
                case INTERACT -> UtilMessage.simpleMessage(player, "Clans", "You cannot use <green>%s <gray>in %s<gray>.", blockName, owner);
                default -> {
                }
            }
        } else {
            switch (event.getInteraction()) {
                case BREAK -> UtilMessage.message(player, "Clans", "Clan Recruits cannot break blocks.");
                case PLACE -> UtilMessage.simpleMessage(player, "Clans", "Clan Recruits cannot place blocks.");
                case INTERACT -> UtilMessage.simpleMessage(player, "Clans", "Clan Recruits cannot access <green>%s<gray>.", blockName);
                default -> {
                }
            }
        }
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

        new CoreMenu(clan, event.getPlayer(), itemFactory).show(event.getPlayer());
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
        if (!this.zoneManager.hasTagAt(event.getEntity().getLocation(), Zones.NO_BUILD)) {
            return;
        }
        if (event.getRemover() instanceof final Player player) {
            final Client client = this.clientManager.search().online(player);
            if (!client.isAdministrating()) {
                event.setCancelled(true);
            }
        }
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
            if (!this.zoneManager.hasTagAt(event.getEntity().getLocation(), Zones.NO_BUILD)) {
                return;
            }
            if (event.getDamager() instanceof final Player player) {
                final Client client = this.clientManager.search().online(player);
                if (!client.isAdministrating()) {
                    event.setCancelled(true);
                }
            }
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
            if (!this.zoneManager.hasTagAt(event.getClickedBlock().getLocation(), Zones.NO_BUILD)) {
                return;
            }
            final Client client = this.clientManager.search().online(event.getPlayer());
            if (!client.isAdministrating()) {
                event.setCancelled(true);
            }
        }
    }

    /*
     * Stops players from taking stuff off armour stands and item frames in
     * admin territory
     */
    @EventHandler
    public void onInteract(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof ItemFrame) {
            if (this.zoneManager.hasTagAt(event.getRightClicked().getLocation(), Zones.NO_BUILD)) {
                final Client client = this.clientManager.search().online(event.getPlayer());
                if (!client.isAdministrating()) {
                    event.setCancelled(true);
                }
            }
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
        BaseItem waterBlock = itemRegistry.getItem(new NamespacedKey(clans, "water_block"));
        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(event.getItemInHand());
        if (event.getBlock().getType() != Material.LAPIS_BLOCK || itemOpt.isEmpty() || !itemOpt.get().getBaseItem().equals(waterBlock)) {
            return;
        }

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

        final Block block = event.getBlock();
        block.setType(Material.WATER);
        block.getLocation().getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
        block.getState().update();


    }

    /*
     * Prevent obsidian from being broken by non admins
     */
    @EventHandler
    public void onBreakObsidian(final BlockBreakEvent event) {

        if (event.getBlock().getType() == Material.OBSIDIAN) {
            final Player player = event.getPlayer();
            event.setCancelled(true);
            if (this.zoneManager.hasTagAt(event.getBlock().getLocation(), Zones.NO_BUILD)) {
                UtilMessage.simpleMessage(player, "Server", "You cannot break <yellow>Obsidian<gray>.");
                return;
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
        if (this.zoneManager.hasTagAt(event.getBlock().getLocation(), Zones.NO_BUILD) || blockTagHandler.isPlayerPlaced(event.getBlock())) {
            event.setCancelled(true);
        }

        final BlockData data = event.getBlock().getBlockData();
        // Persistent (player-placed) leaves should never decay
        if (data instanceof Leaves leaves && leaves.isPersistent()) {
            event.setCancelled(true);
        }

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

        if (this.zoneManager.hasTagAt(player.getLocation(), Zones.SAFE)) {
            final Gamer gamer = this.clientManager.search().online(player).getGamer();
            if (!gamer.isInCombat()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkClaim(final ChunkClaimEvent event) {
        event.getChunk().getPersistentDataContainer().set(ClansNamespacedKeys.CLAN,
                PersistentDataType.LONG,
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
                final Client client = this.clientManager.search().online(player);
                if (!client.isAdministrating() && player.getGameMode() == GameMode.SURVIVAL) {
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFishMechanics(final PlayerFishEvent event) {

        if (event.getCaught() instanceof final Player player) {
            final boolean aggressiveRodderUnlocked = hasAggressiveRodderUnlocked(event.getPlayer());
            if (aggressiveRodderUnlocked) {
                event.getPlayer().removeMetadata(AGGRESSIVE_RODDER_UNLOCKED, this.clans);
            }

            if (!this.energyService.use(event.getPlayer(), "Fishing Rod", 15.0, true)) {
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

            final boolean friendly = targetClan.equals(playerClan) || targetClan.isAllied(playerClan);
            if (!friendly && !aggressiveRodderUnlocked) {
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

    private boolean hasAggressiveRodderUnlocked(final Player player) {
        return player.hasMetadata(AGGRESSIVE_RODDER_UNLOCKED);
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
        if (clan == null) {
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

    @EventHandler
    public void onLavaPlace(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (!event.getItem().getType().equals(Material.LAVA_BUCKET)) return;
        final Client client = this.clientManager.search().online(event.getPlayer());
        if (client.isAdministrating()) return;

        event.setUseItemInHand(Event.Result.DENY);
        event.getPlayer().getInventory().remove(Material.LAVA_BUCKET);
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
            chunk.getPersistentDataContainer().set(ClansNamespacedKeys.CLAN, PersistentDataType.LONG, clan.getId());
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
        BlockData data = block.getBlockData();

        // TODO remove this when players are given persistent leaves again
        // Check if the block is actually a leaf block
        if (data instanceof Leaves leaves) {
            // If persistent is false, the leaf is natural
            leaves.setPersistent(true);
            block.setBlockData(leaves);
        }


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

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleOreReplacements(BlockDropItemEvent event) {
        BlockState block = event.getBlockState();
        if (block.getType() == Material.COPPER_ORE || block.getType() == Material.DEEPSLATE_COPPER_ORE) {
            final ItemStack stack = new ItemStack(Material.LEATHER, 1);
            event.getItems().clear();

            Item item = block.getWorld().dropItemNaturally(block.getLocation(), stack);
            event.getItems().add(item);
        } else if (block.getType() == Material.GILDED_BLACKSTONE) {
            final ItemStack stack = new ItemStack(Material.NETHERITE_INGOT, 1);
            event.getItems().clear();

            Item item = block.getWorld().dropItemNaturally(block.getLocation(), stack);
            event.getItems().add(item);
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
    public void onBlockGrow(BlockGrowEvent event) {
        if (this.zoneManager.hasTagAt(event.getBlock().getLocation(), Zones.NO_BUILD)) {
            event.setCancelled(true);
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

        if (clan.isOnline()) return;

        event.setCancelled(true);
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

    @EventHandler
    public void onMount(PlayerMountEvent playerMountEvent) {
        if (playerMountEvent.isCancelled()) return;

        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(playerMountEvent.getPlayer());
        Optional<Clan> playerLocationClanOptional = clanManager.getClanByLocation(playerMountEvent.getPlayer().getLocation());

        if (playerLocationClanOptional.isEmpty()) {
            return;
        }

        Clan playerLocationClan = playerLocationClanOptional.get();

        ClanRelation relation = clanManager.getRelation(playerLocationClan, playerClanOptional.orElse(null));
        if (relation != ClanRelation.SELF && relation != ClanRelation.ALLY) {
            playerMountEvent.cancel("You cannot mount while in another clans territory");
        }
    }

}
