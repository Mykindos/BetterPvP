package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

@Slf4j
@BPvPListener
public class ClansExplosionListener extends ClanListener {

    private WeakHashMap<Entity, Clan> tntMap = new WeakHashMap<>();

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


    private final WorldBlockHandler worldBlockHandler;

    @Inject
    public ClansExplosionListener(ClanManager clanManager, GamerManager gamerManager, WorldBlockHandler worldBlockHandler) {
        super(clanManager, gamerManager);
        this.worldBlockHandler = worldBlockHandler;
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
        if(!(event.getPrimingEntity() instanceof Player player)) return;
        log.info("{} primed TNT", player.getName());

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if(clanOptional.isPresent()) {
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
                        if(!gamer.getClient().isAdministrating()) {
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

        if(!tntMap.containsKey(event.getEntity())) return;

        if (event.getEntity().getType() != EntityType.PRIMED_TNT) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        event.setYield(2.5f);
        for (Block block : UtilBlock.getInRadius(event.getLocation(), event.getYield()).keySet()) {
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

        for (Block block : event.blockList()) {
            if (protectedBlocks.contains(block.getType())) continue;
            if (worldBlockHandler.isRestoreBlock(block)) continue;

            Optional<Clan> clanOptional = clanManager.getClanByLocation(block.getLocation());
            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();

                Optional<ClanEnemy> enemyOptional = attackingClan.getEnemy(clan);
                if(enemyOptional.isPresent()) {

                    ClanEnemy enemy = enemyOptional.get();
                    if(enemy.getDominance() <= dominanceRequired) {
                        attackingClan.messageClan("You cannot TNT <red>" + clan.getName() + "</red> because you have less than <red>" + dominanceRequired + "%</red> dominance on them.", null, true);
                        break;
                    }

                    if(clan.isNoDominanceCooldownActive()) {
                        attackingClan.messageClan("You cannot TNT <red>" + clan.getName() + "</red> because they are a new clan or were raided too recently.", null, true);
                        break;
                    }

                }else {
                    attackingClan.messageClan("You cannot TNT <yellow>" + clan.getName() + "</yellow> because you are not enemies.", null, true);
                    break;
                }
                if (clan.isAdmin() || clan.isSafe()) continue;
                // TODO clan tnt protection

                clanManager.addInsurance(clan, block, InsuranceType.BREAK);

                if (tntCooldownEnabled) {
                    clan.saveProperty(ClanProperty.LAST_TNTED.name(), (long) (System.currentTimeMillis() + (tntCooldownMinutes * 60_000)));
                }

            }

            if (processTntTieredBlocks(block)) {
                continue;
            }

            block.breakNaturally();

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
