package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class SeismicSlam extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final Set<UUID> active = new HashSet<>();
    private final List<Entity> fallingBlocks = new ArrayList<>();
    private final WeakHashMap<Player, Double> height = new WeakHashMap<>();
    private final WeakHashMap<Player, List<LivingEntity>> immune = new WeakHashMap<>();
    private final HashMap<Chunk, Long> lastSlam = new HashMap<>();

    private int radius;

    @Inject
    public SeismicSlam(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }


    @Override
    public String getName() {
        return "Seismic Slam";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to Activate",
                "",
                "Jump and slam the ground, knocking up all opponents",
                "within a small radius",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (player.isDead()) {
                    iterator.remove();
                    continue;
                }
                if (UtilBlock.isGrounded(player) || player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
                    slam(player);

                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent event) {
        if (lastSlam.containsKey(event.getChunk())) {
            if (!UtilTime.elapsed(lastSlam.get(event.getChunk()), 60000)) {
                for (Entity ent : event.getChunk().getEntities()) {
                    if (ent instanceof FallingBlock) {

                        ent.remove();
                    }
                }

            }
        }
    }

    public void slam(final Player player) {
        lastSlam.put(player.getLocation().getChunk(), System.currentTimeMillis());
        new BukkitRunnable() {
            int i = 0;

            final List<Player> hit = new ArrayList<>();
            final List<Location> blocks = new ArrayList<>();

            @Override
            public void run() {
                if (i == radius) {
                    cancel();
                }

                final Location loc = player.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);

                for (Block block : UtilBlock.getInRadius(player.getLocation().add(0, -1, 0), i, 3).keySet()) {
                    if (!blocks.contains(block.getLocation())) {
                        if ((block.getLocation().getBlockY() == loc.getBlockY() - 1) && !UtilBlock.airFoliage(block) && !UtilBlock.usable(block)
                                && UtilBlock.airFoliage(block.getRelative(BlockFace.UP))) {

                            FallingBlock fb = loc.getWorld().spawnFallingBlock(block.getLocation().clone().add(0.0D, 1.1, 0.0D), Bukkit.createBlockData(block.getType()));
                            blocks.add(block.getLocation());
                            lastSlam.put(block.getLocation().getChunk(), System.currentTimeMillis());
                            fb.setVelocity(new Vector(0.0F, 0.3F, 0.0F));
                            fb.setDropItem(false);
                            fallingBlocks.add(fb);
                        }
                    }
                }

                for (LivingEntity ent : UtilEntity.getNearbyEnemies(player, player.getLocation(), radius)) {
                    if (immune.get(player).contains(ent)) continue;
                    if (ent instanceof Player target) {
                        if (hit.contains(target)) continue;
                        hit.add(target);
                    }

                    immune.get(player).add(ent);
                    UtilVelocity.velocity(ent, 0.3 * ((height.get(player) - ent.getLocation().getY()) * 0.1), 1, 3, true);
                    UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, DamageCause.CUSTOM, 3.0, false, getName()));
                }
                i++;
            }

        }.runTaskTimer(clans, 0, 2);

    }


    @EventHandler
    public void onBlockChangeState(EntityChangeBlockEvent event) {
        if (fallingBlocks.contains(event.getEntity())) {
            event.setCancelled(true);
            fallingBlocks.remove(event.getEntity());
            FallingBlock fb = (FallingBlock) event.getEntity();
            fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_STONE_STEP, 1.0F, 1.0F);
            event.getEntity().remove();
        }
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }

    @Override
    public void activate(Player player, int level) {
        player.setVelocity(new Vector(0, 1.3, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5F, 0.2F);

        UtilServer.runTaskLater(clans, () -> {
            player.setVelocity(player.getLocation().getDirection().multiply(1).add(new Vector(0, -0.5, 0)));
            championsManager.getEffects().addEffect(player, EffectType.NOFALL, 5000);
            height.put(player, player.getLocation().getY());
            active.add(player.getUniqueId());
            immune.put(player, new ArrayList<>());
        }, 15);

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        radius = getConfig("radius", 5, Integer.class);
    }
}
