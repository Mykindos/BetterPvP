package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe.bloodeffects.BloodCircleEffect;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class BloodBarrier extends Skill implements InteractSkill, CooldownSkill, Listener, HealthSkill, TeamSkill, BuffSkill, DefensiveSkill {

    private final HashMap<UUID, ShieldData> shieldDataMap = new HashMap<>();

    @Getter
    private double duration;
    @Getter
    private double range;
    @Getter
    private double damageReduction;
    @Getter
    private int numAttacksToReduce;

    @Getter
    private double healthReduction;
    @Getter
    private double healthReductionPerPlayerAffected;

    @Inject
    public BloodBarrier(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Blood Barrier";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Grant yourself and allies within <val>" + getRange() + "</val> blocks",
                "a barrier which reduces the damage of the next <val>" + getNumAttacksToReduce(),
                "incoming attacks by <val>" + UtilFormat.formatNumber(getDamageReduction() * 100, 0) + "%</val>",
                "",
                "Barrier lasts for <val>" + getDuration() + "</val> seconds, and does not stack",
                "",
                "Cooldown: <val>" + getCooldown(),
                "Health Sacrifice: <val>" + UtilFormat.formatNumber(getHealthReduction(), 1) + " + <val>" + UtilFormat.formatNumber(getHealthReductionPerPlayerAffected(), 1) + " per player affected",
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
                event.setDamage(event.getDamage() * (1 - shieldData.getDamageReduction()));
                shieldData.count--;
            }
        }
    }

    @UpdateEvent
    public void update() {
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

            boolean hasRole = championsManager.getRoles().hasRole(player);
            if ((entry.getValue().hasRole && !hasRole) || (!entry.getValue().hasRole && hasRole)) {
                return true;
            }

            return false;
        });

        shieldDataMap.forEach((key, value) -> {
            Player player = Bukkit.getPlayer(key);
            if (player != null) {

                double oX = Math.sin(player.getTicksLived() / 10d);
                double oZ = Math.cos(player.getTicksLived() / 10d);
                Particle.DUST.builder().location(player.getLocation().add(oX, 0.7, oZ)).extra(0).color(200, 0, 0).receivers(30).spawn();
            }
        });

    }


    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public boolean canUse(Player player) {
        if (player.getHealth() - getHealthReduction() <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName());
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player) {
        double healthReduction = getHealthReduction();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.0f, 1.0f);

        boolean playerHasRole = championsManager.getRoles().hasRole(player);
        shieldDataMap.put(player.getUniqueId(), new ShieldData((long) (getDuration() * 1000), getNumAttacksToReduce(), getDamageReduction(), playerHasRole));
        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRange())) {

            if (player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected()) < 1) {
                break;
            }

            boolean allyHasRole = championsManager.getRoles().hasRole(ally);
            shieldDataMap.put(ally.getUniqueId(), new ShieldData((long) (getDuration() * 1000), getNumAttacksToReduce(), getDamageReduction(), allyHasRole));
            healthReduction += getHealthReductionPerPlayerAffected();
        }

        UtilPlayer.slowHealth(champions, player, -healthReduction, 5, false);

        BloodCircleEffect.runEffect(player.getLocation().add(new Vector(0, 0.1, 0)), getRange(), Color.fromRGB(255, 0, 0), Color.fromRGB(255, 100, 0));

        // Create icon
        final Location cp = player.getLocation().add(new Vector(0, 0.1, 0));
        final Collection<Player> receivers = cp.getWorld().getNearbyPlayers(cp, 48);
        double step = 0.15;
        int sides = 6;
        double in = 2;
        double rad = getRange() * 0.5;
        for (double r = rad; r <= 2 * rad; r *= 1.5) {
            for (int i = 0; i < sides; i++) {
                Location l1 = new Location(cp.getWorld(), cp.getX() + (r - in) * (Math.sin(Math.toRadians(360.0 / sides * (i)))), cp.getY(), cp.getZ() + (r - in) * (Math.cos(Math.toRadians(360.0 / sides * (i)))));
                Location l2 = new Location(cp.getWorld(), cp.getX() + (r - in) * (Math.sin(Math.toRadians(360.0 / sides * (i + 1)))), cp.getY(), cp.getZ() + (r - in) * (Math.cos(Math.toRadians(360.0 / sides * (i + 1)))));
                for (Location l : VectorLine.withStepSize(l1, l2, step).toLocations()) {
                    Particle.DUST_COLOR_TRANSITION.builder()
                            .colorTransition(255, 255, 0, 255, 100, 0)
                            .location(l)
                            .receivers(receivers)
                            .spawn();
                }
            }
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        range = getConfig("range", 8.0, Double.class);

        healthReduction = getConfig("healthReduction", 6.0, Double.class);

        healthReductionPerPlayerAffected = getConfig("healthReductionPerPlayerAffected", 1.0, Double.class);

        duration = getConfig("duration", 20.0, Double.class);

        damageReduction = getConfig("damageReduction", 0.30, Double.class);

        numAttacksToReduce = getConfig("numAttacksToReduce", 3, Integer.class);
    }

    @Data
    private static class ShieldData {

        private final long endTime;

        public int count;

        public double damageReduction;

        public boolean hasRole;

        public ShieldData(long length, int count, double damageReduction, boolean hasRole) {
            this.endTime = System.currentTimeMillis() + length;
            this.count = count;
            this.damageReduction = damageReduction;
            this.hasRole = hasRole;
        }

    }
}

