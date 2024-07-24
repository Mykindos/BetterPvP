package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.ExplosionResult;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

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
            Material.BEACON,
            ClanCore.CORE_BLOCK
    );

    private final WorldBlockHandler worldBlockHandler;

    @Inject
    public ClansExplosionListener(final ClanManager clanManager, final ClientManager clientManager, final WorldBlockHandler worldBlockHandler) {
        super(clanManager, clientManager);
        this.worldBlockHandler = worldBlockHandler;
    }

    /*
     * Prevents players from igniting TNT in Admin Protected Areas
     */
    @EventHandler
    public void preventTnTIgniting(final PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.TNT) return;
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(block.getLocation());
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

    // Cancel damage from TNT if it doesn't have LOS to the entity
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCustomDamage(final PreCustomDamageEvent pre) {
        final CustomDamageEvent event = pre.getCustomDamageEvent();
        //noinspection UnstableApiUsage
        if (event.getDamageSource().getDamageType() != DamageType.PLAYER_EXPLOSION || !(event.getDamagingEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        final Location center = tnt.getLocation().toCenterLocation();
        final Location targetBottom = event.getDamagee().getLocation();
        final Location targetTop = targetBottom.clone().add(0, event.getDamagee().getHeight(), 0);
        if (!hasLineOfSight(center, targetBottom) && !hasLineOfSight(center, targetTop)) {
            pre.setCancelled(true);
            event.setCancelled(true);
        }
    }

    private boolean hasLineOfSight(final @NotNull Location origin, final @NotNull Location target) {
        Preconditions.checkArgument(origin.getWorld().equals(target.getWorld()), "Locations must be in the same world");
        Preconditions.checkArgument(!origin.equals(target), "Locations must not be the same");
        final Vector direction = target.toVector().subtract(origin.toVector());
        final double distance = direction.length();
        final RayTraceResult result = origin.getWorld().rayTraceBlocks(origin, direction, distance, FluidCollisionMode.NEVER, true);
        return result == null || result.getHitBlock() == null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(final EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(true);

        if (!(event.getEntity() instanceof final TNTPrimed tnt)) {
            return;
        }

        final UUID shooterUUID = tnt.getPersistentDataContainer().get(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID);
        if (shooterUUID == null) {
            return;
        }

        final Player shooter = Bukkit.getPlayer(shooterUUID);
        if (shooter == null) {
            return;
        }

        final Clan attackingClan = this.clanManager.getClanByPlayer(shooter).orElse(null);
        if (attackingClan == null) {
            return; // Need to be in a clan to fire a cannon
        }

        Clan attackedClan = null;

        event.setYield(2.5f);
        processBlocksInRadius(event);
        doExplosion(event);

        boolean cannotCannonClan = false;
        for (final Block block : event.blockList()) {
            if (protectedBlocks.contains(block.getType())) continue;

            if (attackedClan == null) {
                final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(block.getLocation());
                if (clanOptional.isPresent()) {
                    attackedClan = clanOptional.get();
                }

            }

            if (attackedClan != null) {
                if (attackedClan.isAdmin() || attackedClan.isSafe()) {
                    break;
                }

                if (!this.clanManager.getPillageHandler().isPillaging(attackingClan, attackedClan)) {
                    cannotCannonClan = true;
                    break;
                }

                this.clanManager.addInsurance(attackedClan, block, InsuranceType.BREAK);
            }

            if (processTntTieredBlocks(block)) {
                continue;
            }

            if (!this.worldBlockHandler.isRestoreBlock(block) && block.getState() instanceof Container) {
                block.breakNaturally(true);
            }

            block.setType(Material.AIR);
            block.getWorld().playEffect(block.getLocation().toCenterLocation(), Effect.STEP_SOUND, block.getType());
        }

        if (cannotCannonClan) {
            UtilMessage.message(shooter, "Clans", "You must pillage this clan first!");
        }
    }

    private void doExplosion(final EntityExplodeEvent event) {
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.0f);
        Particle.EXPLOSION_EMITTER.builder().count(1).location(event.getEntity().getLocation()).spawn();

        UtilServer.callEvent(new BlockExplodeEvent(event.getLocation().getBlock(), event.getLocation().getBlock().getState(), event.blockList(), event.getYield(), ExplosionResult.DESTROY));
    }

    private void processBlocksInRadius(final EntityExplodeEvent event) {
        final Set<Block> blocks = UtilBlock.getInRadius(event.getLocation(), event.getYield()).keySet();
        for (final Block block : blocks) {
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

    private boolean processTntTieredBlocks(final Block block) {

        for (final TNTBlocks tntBlock : TNTBlocks.values()) {
            final List<Material> tiers = tntBlock.getTiers();
            if (!tiers.contains(block.getType())) continue;

            final int index = tiers.indexOf(block.getType());
            if (index == tiers.size() - 1) {
                block.getWorld().playEffect(block.getLocation().toCenterLocation(), Effect.STEP_SOUND, block.getType());
                if (block.getState() instanceof Container) {
                    block.breakNaturally(true);
                }

                block.setType(Material.AIR);
            } else {
                block.setType(tiers.get(index + 1));
            }

            return true;
        }

        return false;
    }

    @EventHandler
    public void onUpdateLore(final ItemUpdateLoreEvent event) {
        final Material material = event.getItemStack().getType();

        for (final TNTBlocks tntBlock : TNTBlocks.values()) {
            if (tntBlock.getTiers().contains(material)) {
                final int resistance = tntBlock.getTiers().size() - tntBlock.getTiers().indexOf(material);
                if(resistance > 1) {
                    event.getItemLore().add(UtilMessage.deserialize("It takes <green>%d</green> cannonballs to destroy this block", resistance));
                } else {
                    event.getItemLore().add(UtilMessage.deserialize("It takes <green>1</green> cannonball to destroy this block"));
                }

                return;
            }
        }

        if(material.isBlock() && !(event.getItem() instanceof Weapon)) {
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

        TNTBlocks(final Material... tiers) {
            this.tiers = Arrays.asList(tiers);
        }


    }
}
