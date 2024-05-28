package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@CustomLog
@BPvPListener
public class ClansExplosionListener extends ClanListener {

    private static final Set<Material> protectedBlocks = Set.of(
            Material.BEDROCK,
            Material.BEACON
    );

    @Inject
    @Config(path = "clans.tnt.enabled", defaultValue = "false")
    private boolean tntEnabled;

    @Inject
    @Config(path = "clans.tnt.cooldown.durationInMinutes", defaultValue = "0.5")
    private double tntCooldownMinutes;

    @Inject
    @Config(path = "clans.tnt.cooldown.enabled", defaultValue = "true")
    private boolean tntCooldownEnabled;

    @Inject
    @Config(path = "clans.tnt.dominanceRequired", defaultValue = "80")
    private int dominanceRequired;

    @Inject
    @Config(path = "clans.tnt.protectionBaseTimeInMinutes", defaultValue = "5.0")
    private double protectionBaseTimeInMinutes;

    @Inject
    @Config(path = "clans.tnt.protectionAdditionalMinutesPerMember", defaultValue = "2.5")
    private double protectionAdditionalMinutesPerMember;

    @Inject
    @Config(path = "clans.tnt.regenerationTimeInMinutes", defaultValue = "5.0")
    private double regenerationTimeInMinutes;

    @Inject
    @Config(path = "clans.pillage.protection", defaultValue = "true")
    private boolean pillageProtection;

    private final Clans clans;
    private final WorldBlockHandler worldBlockHandler;
    private final ItemHandler itemHandler;

    @Inject
    public ClansExplosionListener(ClanManager clanManager, ClientManager clientManager, Clans clans, WorldBlockHandler worldBlockHandler, ItemHandler itemHandler) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.worldBlockHandler = worldBlockHandler;
        this.itemHandler = itemHandler;
    }

    @UpdateEvent(delay = 2000)
    public void updateClanTNTProtection() {
        for (Clan clan : clanManager.getObjects().values()) {
            if (clan.isOnline()) {
                clan.removeProperty(ClanProperty.TNT_PROTECTION.name());
            } else {
                Optional<Long> lastTntedOptional = clan.getProperty(ClanProperty.TNT_PROTECTION);
                if (lastTntedOptional.isEmpty()) {
                    long baseTime = (long) (System.currentTimeMillis() + (protectionBaseTimeInMinutes * 60_000));
                    baseTime = baseTime + (long) ((clan.getMembers().size() - 1) * (protectionAdditionalMinutesPerMember * 60_000));

                    clan.putProperty(ClanProperty.TNT_PROTECTION, baseTime);
                }
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getBlock().getLocation());
        if (playerClanOptional.isPresent() && locationClanOptional.isPresent()) {
            Clan playerClan = playerClanOptional.get();
            Clan locationClan = locationClanOptional.get();

            if (playerClan.equals(locationClan)) {
                if (System.currentTimeMillis() < playerClan.getLastTntedTime()) {
                    final Client client = clientManager.search().online(event.getPlayer());
                    if (!client.isAdministrating()) {
                        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot place blocks for <green>%s</green>.",
                                UtilTime.getTime((playerClan.getLastTntedTime() - System.currentTimeMillis()), 1));
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    /*
     * Prevents players from igniting TNT in Admin Protected Areas
     */
    @EventHandler
    public void preventTnTIgniting(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.TNT) return;
            Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(true);

        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        UUID shooterUUID = tnt.getPersistentDataContainer().get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID);
        if (shooterUUID == null) {
            return;
        }

        Player shooter = Bukkit.getPlayer(shooterUUID);
        if (shooter == null) {
            return;
        }

        Clan attackingClan = clanManager.getClanByPlayer(shooter).orElse(null);
        if (attackingClan == null) {
            return; // Need to be in a clan to fire a cannon
        }

        Clan attackedClan = null;
        boolean schedulingRollback = false;

        processBlocksInRadius(event);
        doExplosion(event);

        for (Block block : event.blockList()) {
            if (protectedBlocks.contains(block.getType())) continue;

            if (attackedClan == null) {
                Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
                if (clanOptional.isPresent()) {
                    attackedClan = clanOptional.get();
                }

            }

            if (attackedClan != null) {

                if (attackedClan.isAdmin() || attackedClan.isSafe()) break;

                Optional<Long> tntProtectionOptional = attackedClan.getProperty(ClanProperty.TNT_PROTECTION);
                if (tntProtectionOptional.isPresent()) {
                    long tntProtection = tntProtectionOptional.get();
                    if (System.currentTimeMillis() > tntProtection) {
                        attackingClan.messageClan("You cannot cannon <red>" + attackedClan.getName() + "</red> because they have offline raid protection.", null, true);
                        refundCannonball(shooter);
                        break;
                    }
                }

                Optional<ClanEnemy> enemyOptional = attackingClan.getEnemy(attackedClan);
                if (enemyOptional.isPresent()) {

                    ClanEnemy enemy = enemyOptional.get();
                    if (enemy.getDominance() <= dominanceRequired) {
                        attackingClan.messageClan("You cannot cannon <red>" + attackedClan.getName() + "</red> because you have less than <red>" + dominanceRequired + "%</red> dominance on them.", null, true);
                        refundCannonball(shooter);
                        break;
                    }

                    if (attackedClan.isNoDominanceCooldownActive() && pillageProtection) {
                        attackingClan.messageClan("You cannot cannon <red>" + attackedClan.getName() + "</red> because they are a new clan or were raided too recently.", null, true);
                        refundCannonball(shooter);
                        break;
                    }

                    if (!clanManager.getPillageHandler().isPillaging(enemy.getClan(), attackedClan)) {
                        schedulingRollback = true;
                    }

                } else {
                    attackingClan.messageClan("You cannot cannon <yellow>" + attackedClan.getName() + "</yellow> because you are not enemies.", null, true);
                    refundCannonball(shooter);
                    break;
                }

                clanManager.addInsurance(attackedClan, block, InsuranceType.BREAK);

                if (tntCooldownEnabled) {
                    attackedClan.saveProperty(ClanProperty.LAST_TNTED.name(), (long) (System.currentTimeMillis() + (tntCooldownMinutes * 60_000)));
                }

            }

            if (processTntTieredBlocks(block)) {
                continue;
            }

            if (!worldBlockHandler.isRestoreBlock(block)) {
                block.breakNaturally();
            } else {
                block.setType(Material.AIR);
            }

        }

        if (schedulingRollback) {
            scheduleRollback(attackingClan, attackedClan);
        }

    }

    private void doExplosion(EntityExplodeEvent event) {
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.0f);
        Particle.EXPLOSION_HUGE.builder().count(1).location(event.getEntity().getLocation()).spawn();
        event.setYield(2.5f);

        final BlockExplodeEvent explodeEvent = new BlockExplodeEvent(event.getLocation().getBlock(), event.blockList(), 1.0f, null);
        UtilServer.callEvent(explodeEvent);
    }

    private void processBlocksInRadius(EntityExplodeEvent event) {
        final Set<Block> blocks = UtilBlock.getInRadius(event.getLocation(), event.getYield()).keySet();
        for (Block block : blocks) {
            if (protectedBlocks.contains(block.getType())) continue;
            if (block.getType().isAir()) continue;

            if (block.isLiquid()) {
                block.setType(Material.AIR);
            } else {
                if (!event.blockList().contains(block)) {
                    event.blockList().add(block);
                }
            }
        }
    }

   private void scheduleRollback(Clan attackingClan, Clan attackedClan) {
       if (attackedClan.getTntRecoveryRunnable() != null) {
           attackedClan.getTntRecoveryRunnable().cancel();
       }

       attackedClan.messageClan("Your clan has been cannoned by <red>" + attackingClan.getName()
               + "</red>! Your blocks will be restored in <green>" + regenerationTimeInMinutes + "</green> minutes.", null, true);

       attackedClan.setTntRecoveryRunnable(UtilServer.runTaskLater(clans, () -> {
           clanManager.startInsuranceRollback(attackedClan);
           attackedClan.messageClan("Commencing restore of blocks destroyed by TNT...", null, true);
       }, (long) (1200L * (regenerationTimeInMinutes))));
    }

    private void refundCannonball(Player player) {
        BPvPItem item = itemHandler.getItem("clans:cannonball");
        if(item != null) {
            ItemStack itemStack = itemHandler.updateNames(item.getItemStack(1));
            UtilItem.insert(player, itemStack);
        }
    }

    private boolean processTntTieredBlocks(Block block) {

        for (TNTBlocks tntBlock : TNTBlocks.values()) {
            List<Material> tiers = tntBlock.getTiers();
            if (!tiers.contains(block.getType())) continue;

            int index = tiers.indexOf(block.getType());
            if (index == tiers.size() - 1) {
                block.breakNaturally();
            } else {
                block.setType(tiers.get(index + 1));
            }

            return true;
        }

        return false;
    }

    @EventHandler
    public void onUpdateLore(ItemUpdateLoreEvent event) {
        Material material = event.getItemStack().getType();

        for (TNTBlocks tntBlock : TNTBlocks.values()) {
            if (tntBlock.getTiers().contains(material)) {
                int resistance = tntBlock.getTiers().size() - tntBlock.getTiers().indexOf(material);
                if(resistance > 1) {
                    event.getItemLore().add(UtilMessage.deserialize("It takes <green>%d</green> cannonballs to destroy this block", resistance));
                } else {
                    event.getItemLore().add(UtilMessage.deserialize("It takes <green>1</green> cannonball to destroy this block"));
                }

                return;
            }
        }

        if(material.isBlock()) {
            if(event.getItem().getLore(null).isEmpty()) {
                event.getItemLore().add(UtilMessage.deserialize("It takes <green>1</green> cannonball to destroy this block"));
            }
        }
    }

    @Getter
    private enum TNTBlocks {

        STONEBRICK(Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS),
        NETHERBRICKS(Material.NETHER_BRICKS, Material.NETHERRACK),
        SANDSTONE(Material.SMOOTH_SANDSTONE, Material.SANDSTONE),
        REDSANDSTONE(Material.SMOOTH_RED_SANDSTONE, Material.RED_SANDSTONE),
        BLACKSTONE(Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS),
        QUARTZ(Material.QUARTZ_BRICKS, Material.CHISELED_QUARTZ_BLOCK),
        PURPUR(Material.PURPUR_BLOCK, Material.PURPUR_PILLAR),
        ENDSTONE(Material.END_STONE_BRICKS, Material.END_STONE),
        MOSSYSTONEBRICK(Material.MOSSY_STONE_BRICKS, Material.MOSSY_COBBLESTONE),
        GLASS(Material.TINTED_GLASS, Material.GLASS),
        PRISMARINE(Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS, Material.PRISMARINE),
        /*
        To replace prismarine when 1.21.0 comes out, these blocks have 3 variants, look better, and can also be used as the vault block

        COPPER(Material.CHISELED_COPPER, Material.COPPER_BLOCK, Material.COPPER_GRATE),
        COPPEREXPOSED(Material.EXPOSED_CHISELED_COPPER, Material.EXPOSED_COPPER_BLOCK, Material.EXPOSED_COPPER_GRATE),
        COPPERWEATHERED(Material.WEATHERED_CHISELED_COPPER, Material.WEATHERED_COPPER_BLOCK, Material.WEATHERED_COPPER_GRATE),
        COPPEROXIDIZED(Material.OXIDIZED_CHISELED_COPPER, Material.OXIDIZED_COPPER_BLOCK, Material.OXIDIZED_COPPER_GRATE),
        COPPERWAXED(Material.WAXED_CHISELED_COPPER, Material.WAXED_COPPER_BLOCK, Material.WAXED_COPPER_GRATE),
        COPPERWAXEDEXPOSED(Material.WAXED_EXPOSED_CHISELED_COPPER, Material.WAXED_EXPOSED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER_GRATE),
        COPPERWAXEDWEATHERED(Material.WAXED_WEATHERED_CHISELED_COPPER, Material.WAXED_WEATHERED_COPPER_BLOCK, Material.WAXED_WEATHERED_COPPER_GRATE),
        COPPERWAXEDOXIDIZED(Material.WAXED_OXIDIZED_CHISELED_COPPER, Material.WAXED_OXIDIZED_COPPER_BLOCK, Material.WAXED_OXIDIZED_COPPER_GRATE),

        TUFFBRICKS(Material.TUFF_BRICKS, Material.TUFF),

        PRISMARINE(Material.PRISMARINE_BRICKS, Material.PRISMARINE),
        */
        MUDBRICKS(Material.MUD_BRICKS, Material.MUD),
        DEEPSLATEBRICKS(Material.DEEPSLATE_BRICKS, Material.DEEPSLATE);


        private final List<Material> tiers;

        TNTBlocks(Material... tiers) {
            this.tiers = Arrays.asList(tiers);
        }


    }
}
