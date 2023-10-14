package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

@Slf4j
@BPvPListener
public class ClansExplosionListener extends ClanListener {

    private final WeakHashMap<Entity, Clan> tntMap = new WeakHashMap<>();

    private static final Set<Material> protectedBlocks = Set.of(
            Material.BEDROCK,
            Material.BEACON
    );

    @Inject
    @Config(path = "clans.tnt.enabled", defaultValue = "false")
    private boolean tntEnabled;

    @Inject
    @Config(path = "clans.tnt.cooldown.durationInMinutes", defaultValue = "5.0")
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


    private final WorldBlockHandler worldBlockHandler;
    private final Clans clans;

    @Inject
    public ClansExplosionListener(ClanManager clanManager, GamerManager gamerManager, WorldBlockHandler worldBlockHandler, Clans clans) {
        super(clanManager, gamerManager);
        this.worldBlockHandler = worldBlockHandler;
        this.clans = clans;
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

    //@EventHandler
    //public void onTNTPlace(BlockPlaceEvent e) {
    //    if (e.getBlock().getType() == Material.TNT) {
    //        if (!tntEnabled) {
    //            UtilMessage.message(e.getPlayer(), "TNT", "TNT is disabled for the first 3 days of each season.");
    //            e.setCancelled(true);
    //        }
    //    }
    //}

    // TODO probably remove this when we add cannons
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {
        event.getBlock().setType(Material.AIR);
        if (!(event.getPrimingEntity() instanceof Player player)) return;
        log.info("{} primed TNT", player.getName());

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isPresent()) {
            TNTPrimed tnt = event.getBlock().getWorld().spawn(event.getBlock().getLocation(), TNTPrimed.class);
            tntMap.put(tnt, clanOptional.get());
            event.setCancelled(true);
        } else {
            UtilMessage.message(player, "TNT", "You must be in a clan to prime TNT.");
            event.setCancelled(true);
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
                    gamerManager.getObject(event.getPlayer().getUniqueId().toString()).ifPresent(gamer -> {
                        if (!gamer.getClient().isAdministrating()) {
                            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot place blocks for <green>%s</green>.",
                                    UtilTime.getTime(playerClan.getLastTntedTime() - System.currentTimeMillis(), UtilTime.TimeUnit.BEST, 1));
                            event.setCancelled(true);
                        }
                    });

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
                    Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
                    gamerOptional.ifPresent(gamer -> {
                        if (!gamer.getClient().isAdministrating()) {
                            event.setCancelled(true);
                        }
                    });
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(true);

        if (!tntMap.containsKey(event.getEntity())) return;

        if (event.getEntity().getType() != EntityType.PRIMED_TNT) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        event.setYield(2.5f);
        final Set<Block> blocks = UtilBlock.getInRadius(event.getLocation(), event.getYield()).keySet();
        for (Block block : blocks) {
            if (protectedBlocks.contains(block.getType())) continue;
            if (worldBlockHandler.isRestoreBlock(block)) continue;
            if (block.getType().isAir()) continue;

            if (block.isLiquid()) {
                block.setType(Material.AIR);
            } else {
                if (!(block.getState() instanceof Container) && !block.getType().isInteractable()) {
                    if (!event.blockList().contains(block)) {
                        event.blockList().add(block);
                    }
                }
            }
        }

        Clan attackingClan = tntMap.get(event.getEntity());
        Clan attackedClan = null;

        boolean schedulingRollback = false;

        // todo: allow changing yield
        final BlockExplodeEvent explodeEvent = new BlockExplodeEvent(event.getLocation().getBlock(), new ArrayList<>(blocks), 1.0f, null);
        UtilServer.callEvent(explodeEvent);

        for (Block block : event.blockList()) {
            if (protectedBlocks.contains(block.getType())) continue;
            if (worldBlockHandler.isRestoreBlock(block)) continue;

            if (attackedClan == null) {
                Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
                if (clanOptional.isPresent()) {
                    attackedClan = clanOptional.get();
                }

            } else {

                Optional<Long> tntProtectionOptional = attackedClan.getProperty(ClanProperty.TNT_PROTECTION);
                if (tntProtectionOptional.isPresent()) {
                    long tntProtection = tntProtectionOptional.get();
                    if (System.currentTimeMillis() > tntProtection) {
                        attackingClan.messageClan("You cannot TNT <red>" + attackedClan.getName() + "</red> because they have offline TNT protection</red>.", null, true);
                        break;
                    }
                }

                Optional<ClanEnemy> enemyOptional = attackingClan.getEnemy(attackedClan);
                if (enemyOptional.isPresent()) {

                    ClanEnemy enemy = enemyOptional.get();
                    if (enemy.getDominance() <= dominanceRequired) {
                        attackingClan.messageClan("You cannot TNT <red>" + attackedClan.getName() + "</red> because you have less than <red>" + dominanceRequired + "%</red> dominance on them.", null, true);
                        break;
                    }

                    if (attackedClan.isNoDominanceCooldownActive()) {
                        attackingClan.messageClan("You cannot TNT <red>" + attackedClan.getName() + "</red> because they are a new clan or were raided too recently.", null, true);
                        break;
                    }

                    if (!clanManager.getPillageHandler().isPillaging(enemy.getClan(), attackedClan)) {
                        schedulingRollback = true;
                    }

                } else {
                    attackingClan.messageClan("You cannot TNT <yellow>" + attackedClan.getName() + "</yellow> because you are not enemies.", null, true);
                    break;
                }
                if (attackedClan.isAdmin() || attackedClan.isSafe()) continue;


                clanManager.addInsurance(attackedClan, block, InsuranceType.BREAK);

                if (tntCooldownEnabled) {
                    attackedClan.saveProperty(ClanProperty.LAST_TNTED.name(), (long) (System.currentTimeMillis() + (tntCooldownMinutes * 60_000)));
                }


            }

            if (processTntTieredBlocks(block)) {
                continue;
            }

            block.breakNaturally();

        }

        if (schedulingRollback) {
            if (attackedClan.getTntRecoveryRunnable() != null) {
                attackedClan.getTntRecoveryRunnable().cancel();
            }

            attackedClan.messageClan("Your clan has been TNT'd by <red>" + attackingClan.getName()
                    + "</red>! Your blocks will be restored in <green>" + regenerationTimeInMinutes + "</green> minutes.", null, true);

            Clan finalAttackedClan = attackedClan;
            attackedClan.setTntRecoveryRunnable(UtilServer.runTaskLater(clans, () -> {
                clanManager.startInsuranceRollback(finalAttackedClan);
                finalAttackedClan.messageClan("Commencing restore of blocks destroyed by TNT...", null, true);
            }, (long) (1200L * (regenerationTimeInMinutes))));
        }

        tntMap.remove(event.getEntity());
    }

    @EventHandler
    public void onTNTPrimed(TNTPrimeEvent event) {

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
        PRISMARINE(Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS, Material.PRISMARINE);


        private final List<Material> tiers;

        TNTBlocks(Material... tiers) {
            this.tiers = Arrays.asList(tiers);
        }


    }
}
