package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Singleton
@BPvPListener
public class Illusion extends Skill implements CooldownSkill, InteractSkill, Listener {

    private double baseDuration;
    private Map<Player, Skeleton> activeIllusions = new HashMap<>();
    private Map<Player, Long> swapCooldowns = new HashMap<>();
    private static final long SWAP_COOLDOWN_DURATION = 1500;
    private Map<Player, Integer> illusionWalkingTasks = new HashMap<>();
    private Map<Player, Long> illusionStartTime = new HashMap<>();

    @Inject
    public Illusion(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        Bukkit.getPluginManager().registerEvents(this, champions);

    }

    @Override
    public String getName() {
        return "Illusion";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Send out a copy of yourself that will run forwards in whatever direction you look",
                "Right clicking while the illusion is alive will swap places with it",
                "",
                "There is an internal cooldown of 1.5 second to illusion swapping",
                "",
                "Illusion lasts <val>" + (baseDuration + (level)) + "</val> seconds and can be destroyed",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 3);
    }

    private void bodySwap(Player player, Boolean force) {
        if (!championsManager.getCooldowns().hasCooldown(player, "Body Swap") || force) {

            if (!force) {
                UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>Body Swap " + getLevel(player) + "</alt>.");
            } else {
                championsManager.getCooldowns().removeCooldown(player, "Illusion", true);
                championsManager.getCooldowns().use(player, "Illusion", 2, true);
            }

            if (swapCooldowns.containsKey(player)) {
                long lastSwapTime = swapCooldowns.get(player);
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - lastSwapTime;

                if (timePassed < SWAP_COOLDOWN_DURATION) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>Body Swap</alt> cannot be used for " + String.format("%.1f", (SWAP_COOLDOWN_DURATION - timePassed) / 1000.0) + " seconds");
                    return;
                }
            }

            Skeleton illusion = activeIllusions.get(player);
            Location tempLoc = player.getLocation().clone();
            player.teleport(illusion);
            illusion.teleport(tempLoc);

            swapCooldowns.put(player, System.currentTimeMillis());
        }
    }

    @Override
    public void activate(Player player, int level) {
        Skeleton illusion = player.getWorld().spawn(player.getLocation(), Skeleton.class);
        setupIllusionAttributes(player, illusion);

        scheduleIllusionUpdate(player, illusion);

        activeIllusions.put(player, illusion);
        illusionStartTime.put(player, System.currentTimeMillis());

        scheduleIllusionRemoval(player, illusion, level);

        Integer taskID = scheduleIllusionWalkingTask(player, illusion);

        illusionWalkingTasks.put(player, taskID);

        scheduleWalkingTaskCancellation(player, illusion, level, taskID);
    }

    public boolean canUse(Player player) {
        if ((illusionStartTime.containsKey(player) && activeIllusions.containsKey(player)) && (!UtilTime.elapsed(illusionStartTime.get(player), (long)((baseDuration + getLevel(player)) * 1000L)))) {
                bodySwap(player, false);
                return false;
            }
        return true;
    }

    public void setupIllusionAttributes(Player player, Skeleton illusion) {
        illusion.setAI(true);
        Vector direction = player.getLocation().getDirection().multiply(0.2);
        direction.setY(-0.5);
        illusion.setVelocity(direction);
        illusion.getEquipment().setArmorContents(player.getEquipment().getArmorContents());
        illusion.getEquipment().setItemInMainHand(player.getEquipment().getItemInMainHand());
        illusion.setHealth(10.0);
    }

    public void scheduleIllusionUpdate(Player player, Skeleton illusion) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(champions, () -> {
            if (activeIllusions.containsKey(player) && illusion.isValid()) {
                illusion.getEquipment().setItemInMainHand(player.getEquipment().getItemInMainHand());
                illusion.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
            }
        }, 0, 5);
    }

    @EventHandler
    public void onIllusionFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Skeleton) {
            Skeleton damagedSkeleton = (Skeleton) event.getEntity();
            if (activeIllusions.containsValue(damagedSkeleton) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }
        }
    }

    private void removeIllusionWithEffects(Skeleton illusion) {
        illusion.getWorld().spawnParticle(Particle.SMOKE_LARGE, illusion.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        illusion.getWorld().playSound(illusion.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);
        illusion.remove();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (activeIllusions.containsKey(player)) {
            Skeleton illusion = activeIllusions.get(player);
            if (illusion.isValid()) {
                removeIllusionWithEffects(illusion);
            }
            activeIllusions.remove(player);
            if (illusionWalkingTasks.containsKey(player)) {
                Bukkit.getScheduler().cancelTask(illusionWalkingTasks.get(player));
                illusionWalkingTasks.remove(player);
            }
        }
        swapCooldowns.remove(player);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Skeleton) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            if (activeIllusions.containsValue(skeleton)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onIllusionDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Skeleton) {
            Skeleton damagedSkeleton = (Skeleton) event.getEntity();
            if (activeIllusions.containsValue(damagedSkeleton)) {
                Player owner = getKeyByValue(activeIllusions, damagedSkeleton);
                if (event.getDamager() instanceof Player) {
                    Player damager = (Player) event.getDamager();
                    if (damager.equals(owner)) {
                        event.setCancelled(true);
                        UtilMessage.simpleMessage(damager, getClassType().getName(), "You cannot damage your own illusion.");
                    }
                }
            }
        }
    }

    public void scheduleIllusionRemoval(Player player, Skeleton illusion, int level) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(champions, () -> {
            if (activeIllusions.containsKey(player)) {
                removeIllusionWithEffects(illusion);
                UtilMessage.simpleMessage(player, getClassType().getName(), "Your illusion disappeared.");
                activeIllusions.remove(player);
            }
        }, (long) (baseDuration + level) * 20);
    }

    public Integer scheduleIllusionWalkingTask(Player player, Skeleton illusion) {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(champions, () -> {
            if (activeIllusions.containsKey(player) && illusion.isValid()) {
                Vector taskDirection = player.getLocation().getDirection().multiply(0.2);
                taskDirection.setY(-0.5);
                illusion.setVelocity(taskDirection);
            }
        }, 1L, 1L);
    }

    public void scheduleWalkingTaskCancellation(Player player, Skeleton illusion, int level, Integer taskID) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(champions, () -> {
            illusion.remove();
            activeIllusions.remove(player);
            if (illusionWalkingTasks.containsKey(player)) {
                Bukkit.getScheduler().cancelTask(taskID);
                illusionWalkingTasks.remove(player);
            }
        }, (long) (baseDuration + level) * 20);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeIllusions.containsKey(player)) {
            Skeleton illusion = activeIllusions.get(player);
            if (illusion.isValid()) {
                illusion.remove();
            }
            activeIllusions.remove(player);
            if (illusionWalkingTasks.containsKey(player)) {
                Bukkit.getScheduler().cancelTask(illusionWalkingTasks.get(player));
                illusionWalkingTasks.remove(player);
            }
        }

        swapCooldowns.remove(player);
    }

    @EventHandler
    public void onIllusionDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Skeleton && activeIllusions.containsValue(entity)) {
            Player owner = getKeyByValue(activeIllusions, (Skeleton) entity);
            if (owner != null) {
                UtilMessage.simpleMessage(owner, getClassType().getName(), "Your illusion was killed.");
                activeIllusions.remove(owner);
                removeIllusionWithEffects((Skeleton) entity);
                if (illusionWalkingTasks.containsKey(owner)) {
                    Bukkit.getScheduler().cancelTask(illusionWalkingTasks.get(owner));
                    illusionWalkingTasks.remove(owner);
                }
            }
            event.getDrops().clear();
        }
    }

    private Player getKeyByValue(Map<Player, Skeleton> map, Skeleton value) {
        for (Map.Entry<Player, Skeleton> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
    }
}