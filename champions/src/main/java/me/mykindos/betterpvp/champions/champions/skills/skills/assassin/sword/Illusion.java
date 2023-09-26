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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;

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
    private static final long SWAP_COOLDOWN_DURATION = 1000;

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
                "There is an internal cooldown of 1 second to illusion swapping",
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
    @Override
    public void activate(Player player, int level) {
        if (activeIllusions.containsKey(player)) {

            if (swapCooldowns.containsKey(player)) {
                long lastSwapTime = swapCooldowns.get(player);
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - lastSwapTime;

                if (timePassed < SWAP_COOLDOWN_DURATION) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>Body Swap</alt> cannot be used for " + (SWAP_COOLDOWN_DURATION - timePassed) / 1000 + " seconds");
                    return;
                }
            }

            Skeleton illusion = activeIllusions.get(player);
            Location tempLoc = player.getLocation().clone();
            player.teleport(illusion);
            illusion.teleport(tempLoc);

            swapCooldowns.put(player, System.currentTimeMillis());
        }else {
            Skeleton illusion = player.getWorld().spawn(player.getLocation(), Skeleton.class);
            illusion.setAI(true);
            illusion.setVelocity(player.getLocation().getDirection().multiply(1.2)); // running speed
            illusion.getEquipment().setArmorContents(player.getEquipment().getArmorContents());
            illusion.getEquipment().setItemInMainHand(player.getEquipment().getItemInMainHand());

            Bukkit.getScheduler().scheduleSyncRepeatingTask(champions, () -> {
                if (activeIllusions.containsKey(player) && illusion.isValid()) {
                    illusion.getEquipment().setItemInMainHand(player.getEquipment().getItemInMainHand());
                    illusion.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
                }
            }, 0, 5);

            activeIllusions.put(player, illusion);

            Bukkit.getScheduler().scheduleSyncDelayedTask(champions, () -> {
                illusion.remove();
                activeIllusions.remove(player);
            }, (long) (baseDuration + level) * 20);
        }
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

            entity.getWorld().spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);

            if (owner != null) {
                owner.sendMessage("Your illusion has disappeared");

                activeIllusions.remove(owner);
            }
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