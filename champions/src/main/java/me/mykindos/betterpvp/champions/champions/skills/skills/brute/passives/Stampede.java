package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.StampedeData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@CustomLog
public class Stampede extends Skill implements PassiveSkill, MovementSkill, DamageSkill {

    private final WeakHashMap<Player, StampedeData> playerData = new WeakHashMap<>();
    @Getter
    private double durationPerStack;
    @Getter
    private double damage;
    private int maxSpeedStrength;
    private double knockback;

    @Inject
    public Stampede(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Stampede";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "You slowly build up speed as you",
                "sprint, gaining one level of <effect>Speed</effect>",
                "for every <val>" + getDurationPerStack() + "</val> seconds, up to a max",
                "of <effect>Speed " + UtilFormat.getRomanNumeral(maxSpeedStrength) + "</effect>",
                "",
                "Attacking during stampede deals <val>" + getDamage() + "</val> bonus",
                "bonus damage and <val>" + UtilFormat.formatNumber(getBonusKnockback() * 100, 1) + "</val> extra knockback",
                "per speed level"
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        playerData.remove(player);
    }

    public double getBonusKnockback() {
        return knockback;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @UpdateEvent(delay = 200)
    public void updateSpeed() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isSprinting() && !playerData.containsKey(player)) {
                if (hasSkill(player)) {
                    startStampede(player);
                }
            }
        }

        Iterator<Map.Entry<Player, StampedeData>> iterator = playerData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, StampedeData> entry = iterator.next();
            Player player = entry.getKey();
            StampedeData data = entry.getValue();
            if (!hasSkill(player)) {
                removeSpeed(player);
                iterator.remove();
                continue;
            }

            boolean isSprintingNow = player.isSprinting() && !player.isInWater();

            if (data == null) {
                data = new StampedeData(System.currentTimeMillis(), 0);
                playerData.put(player, data);
            } else if (isSprintingNow && !player.isSneaking() && !UtilBlock.isInLiquid(player)) {
                if (UtilTime.elapsed(data.getSprintTime(), (long) ((getDurationPerStack()) * 1000L))) {
                    if (data.getSprintStrength() < maxSpeedStrength) {
                        data.setSprintTime(System.currentTimeMillis());
                        data.setSprintStrength(data.getSprintStrength() + 1);

                        championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), data.getSprintStrength(), 2200, true);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2.0F, 0.2F * data.getSprintStrength() + 1.2F);
                        UtilMessage.simpleMessage(player, getClassType().getName(), "Stampede Level: <yellow>%d", data.getSprintStrength());
                        spawnParticles(player.getLocation(), (maxSpeedStrength + 2 + 3) * 2);
                    }
                }
                if (data.getSprintStrength() > 0) {
                    championsManager.getEffects().addEffect(player, player, EffectTypes.SPEED, getName(), data.getSprintStrength(), 2200, true);
                    spawnParticles(player.getLocation(), (data.getSprintStrength() + 3) * 2);
                }
            } else {
                removeSpeed(player);
                iterator.remove();
            }
        }

    }

    private void spawnParticles(Location location, int count) {
        for (int i = 0; i < count; i++) {
            Particle.ENTITY_EFFECT.builder()
                    .location(location.clone()
                            .add(UtilMath.randDouble(-0.2, 0.2), UtilMath.randDouble(0.0, 0.1), UtilMath.randDouble(-0.2, 0.2)))
                    .receivers(60)
                    .data(Color.WHITE)
                    .spawn();
        }

    }

    public void removeSpeed(Player player) {
        championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
    }

    private void startStampede(Player player) {
        StampedeData data = playerData.getOrDefault(player, new StampedeData(System.currentTimeMillis(), 0));
        data.setSprintTime(System.currentTimeMillis());
        playerData.put(player, data);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) return;
        playerData.remove(damagee);
        removeSpeed(damagee);
        startStampede(damagee);
    }

    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (damager.isSneaking()) return;

        StampedeData data = playerData.get(damager);
        if (data == null || data.getSprintStrength() < 1) return;

        event.setKnockback(false);
        int str = data.getSprintStrength();

        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 1.5F);
        double knockbackMultiplier = 0.5 + (getBonusKnockback() * str);


        VelocityData velocityData = new VelocityData(UtilVelocity.getTrajectory2d(damager, event.getDamagee()), knockbackMultiplier, true, 0.0D, 0.4D, 1.0D, false);
        UtilVelocity.velocity(event.getDamagee(), damager, velocityData, VelocityType.KNOCKBACK);
        double additionalDamage = getDamage() * str;
        event.setDamage(event.getDamage() + additionalDamage);

        playerData.remove(damager);
        removeSpeed(damager);
        startStampede(damager);
    }

    @Override
    public void loadSkillConfig() {
        durationPerStack = getConfig("durationPerStack", 5.0, Double.class);
        damage = getConfig("damage", 0.5, Double.class);
        maxSpeedStrength = getConfig("maxSpeedStrength", 3, Integer.class);
        knockback = getConfig("knockback", 0.25, Double.class);
    }
}
