package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class BloodBarrier extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final HashMap<UUID, ShieldData> shieldDataMap = new HashMap<>();

    private double duration;
    private int range;

    private double damageReduction;

    private int numAttacksToReduce;

    @Inject
    public BloodBarrier(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Blood Barrier";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Sacrifice <val>" + UtilMath.round(100 - (0.50 + (level * 0.05)) * 100, 2) + "%</val> of your health to grant yourself and",
                "allies within <val>" + (range + level) + "</val> blocks a barrier which reduces the damage",
                "of the next <stat>" + numAttacksToReduce + "</stat> incoming attacks by <stat>" + (damageReduction * 100) + "%</stat>",
                "",
                "Barrier lasts for <stat>" + duration + "</stat>, and does not stack",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    @EventHandler
    public void removeOnDeath(PlayerDeathEvent event) {
        shieldDataMap.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (event.getDamagee() instanceof Player player) {

            ShieldData shieldData = shieldDataMap.get(player.getUniqueId());
            if (shieldData != null) {
                event.setDamage(event.getDamage() * (1 - damageReduction));
                shieldData.count--;
            }
        }
    }

    @UpdateEvent
    public void updateParticles() {
        if (shieldDataMap.isEmpty()) return;
        shieldDataMap.entrySet().removeIf(entry -> {
            if (entry.getValue().count <= 0) {
                return true;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) return true;

            if (entry.getValue().getEndTime() - System.currentTimeMillis() <= 0) {
                UtilMessage.message(player, getClassType().getName(), "Your blood barrier has expired.");
                return true;
            }

            return false;
        });

        shieldDataMap.forEach((key, value) -> {
            Player player = Bukkit.getPlayer(key);
            if (player != null) {

                double oX = Math.sin(player.getTicksLived() / 10d);
                double oZ = Math.cos(player.getTicksLived() / 10d);
                Particle.REDSTONE.builder().location(player.getLocation().add(oX, 0.7, oZ)).extra(0).color(200, 0, 0).receivers(30).spawn();
            }
        });

    }


    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * 2);
    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        double healthReduction = 0.50 + (level * 0.05);
        double proposedHealth = player.getHealth() - (UtilPlayer.getMaxHealth(player) - (UtilPlayer.getMaxHealth(player) * healthReduction));

        if (proposedHealth <= 0.5) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int level) {
        double healthReduction = 0.50 + (level * 0.05);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));
        player.setHealth(Math.max(0.5, proposedHealth));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.0f, 1.0f);

        shieldDataMap.put(player.getUniqueId(), new ShieldData((long) (duration * 1000), numAttacksToReduce));
        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), range + level)) {
            shieldDataMap.put(ally.getUniqueId(), new ShieldData((long) (duration * 1000), numAttacksToReduce));
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        range = getConfig("range", 8, Integer.class);
        duration = getConfig("duration", 60.0, Double.class);
        damageReduction = getConfig("damageReduction", 0.30, Double.class);
        numAttacksToReduce = getConfig("numAttacksToReduce", 3, Integer.class);
    }

    @Data
    private static class ShieldData {

        private final long endTime;

        public int count = 3;

        public ShieldData(long length, int count) {
            this.endTime = System.currentTimeMillis() + length;
            this.count = count;
        }

    }
}

