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
import org.bukkit.entity.Creature;
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
public class Clone extends Skill implements CooldownSkill, InteractSkill, Listener {

    private double baseDuration;
    private Map<Player, Skeleton> activeIllusions = new HashMap<>();
    private Map<Player, Long> swapCooldowns = new HashMap<>();
    private double swapCooldown;
    private Map<Player, Long> illusionStartTime = new HashMap<>();

    @Inject
    public Clone(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        Bukkit.getPluginManager().registerEvents(this, champions);
    }

    @Override
    public String getName() {
        return "Clone";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Launch a clone of yourself in the direction you are looking",
                "Right clicking while the clone is alive will swap places with it",
                "",
                "There is a <stat>" + swapCooldown + "</val>second internal cooldown to swapping",
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

                if (timePassed < (long)(swapCooldown * 1000)) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>Body Swap</alt> cannot be used for " + String.format("%.1f", ((long)(swapCooldown * 1000) - timePassed) / 1000.0) + " seconds");
                    return;
                }
            }

            Skeleton illusion = activeIllusions.get(player);

            Location tempIllusionLoc = illusion.getLocation().clone();
            Location tempPlayerLoc = player.getLocation().clone();

            //so that the player is never inside of the illusion
            tempIllusionLoc.add(0, 10, 0);
            illusion.teleport(tempIllusionLoc);
            player.teleport(tempIllusionLoc);
            illusion.teleport(tempPlayerLoc);

            player.getWorld().spawnParticle(Particle.PORTAL, tempPlayerLoc, 100);
            player.getWorld().spawnParticle(Particle.PORTAL, tempIllusionLoc, 100);
            player.getWorld().playSound(tempPlayerLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);

            swapCooldowns.put(player, System.currentTimeMillis());
        }
    }

    @Override
    public void activate(Player player, int level) {
        Skeleton illusion = player.getWorld().spawn(player.getLocation(), Skeleton.class);
        setupIllusionAttributes(player, illusion);

        scheduleIllusionUpdate(player, illusion);
        scheduleIllusionUpdate(player, illusion);
        updateIllusionTarget(player, illusion);

        activeIllusions.put(player, illusion);
        illusionStartTime.put(player, System.currentTimeMillis());

        scheduleIllusionRemoval(player, illusion, level);
    }

    public boolean canUse(Player player) {
        if ((illusionStartTime.containsKey(player) && activeIllusions.containsKey(player)) && (!UtilTime.elapsed(illusionStartTime.get(player), (long)((baseDuration + getLevel(player)) * 1000L)))) {
            bodySwap(player, false);
            return false;
        }
        return true;
    }

    private Player getNearestPlayer(Player owner, Skeleton illusion) {
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(owner)) {
                double distance = player.getLocation().distance(illusion.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
        }

        return closestPlayer;
    }

    public void updateIllusionTarget(Player player, Skeleton illusion) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(champions, () -> {
            if (activeIllusions.containsKey(player) && illusion.isValid()) {
                Player targetPlayer = getNearestPlayer(player, illusion);
                if (targetPlayer != null) {
                    ((Creature) illusion).setTarget(targetPlayer);
                }
            }
        }, 0, 20);
    }

    public void setupIllusionAttributes(Player player, Skeleton illusion) {
        illusion.setAI(true);
        Vector direction = player.getLocation().getDirection().multiply(1.2);
        illusion.setVelocity(direction);
        Player targetPlayer = getNearestPlayer(player, illusion);
        if (targetPlayer != null) {
            ((Creature) illusion).setTarget(targetPlayer);
        }
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeIllusions.containsKey(player)) {
            Skeleton illusion = activeIllusions.get(player);
            if (illusion.isValid()) {
                illusion.remove();
            }
            activeIllusions.remove(player);
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
        swapCooldown = getConfig("swapCooldown",1.0, Double.class);
    }
}

//fix being able to teleport between dimensions
//fix illusion targeting your allies
//player can shoot their own illusion with arrows