package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

@Singleton
@BPvPListener
public class DefensiveAura extends Skill implements InteractSkill, CooldownSkill, HealthSkill, DefensiveSkill, TeamSkill, BuffSkill {

    @Getter
    private double radius;
    @Getter
    private double duration;
    private int healthBoostStrength;

    @Inject
    public DefensiveAura(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Defensive Aura";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Gives you, and all allies within <val>" + getRadius() + "</val> blocks",
                "<effect>Health Boost " + UtilFormat.getRomanNumeral(healthBoostStrength) + "</effect> for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.HEALTH_BOOST.getDescription(healthBoostStrength)
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }


    @Override
    public void activate(Player player) {
        championsManager.getEffects().addEffect(player, player, EffectTypes.HEALTH_BOOST, healthBoostStrength, (long) (getDuration() * 1000L));
        AttributeInstance playerMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        player.playSound(player, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.8f);
        if (playerMaxHealth != null) {
            UtilPlayer.health(player, 4d * healthBoostStrength);
            for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRadius())) {

                championsManager.getEffects().addEffect(target, player, EffectTypes.HEALTH_BOOST, healthBoostStrength, (long) (getDuration() * 1000L));
                AttributeInstance targetMaxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (targetMaxHealth != null) {
                    UtilPlayer.health(target, 4d * healthBoostStrength);
                    UtilMessage.simpleMessage(target, getClassType().getPrefix(), "<yellow>%s</yellow> cast <green>%s</green> on you!", player.getName(), getName());
                    target.playSound(target, Sound.ENTITY_VILLAGER_WORK_CLERIC, 1f, 1.1f);
                    target.spawnParticle(Particle.HEART, target.getLocation().add(new Vector(0, 1, 0)), 6, 0.5, 0.5, 0.5);
                }
            }
        }

        new BukkitRunnable() {
            final double rIncrement = 1.0;
            double r = rIncrement;
            int count = 0;
            final Location center = player.getLocation();
            final int colorIncrement = (int) (255 / getRadius() * rIncrement);

            final Collection<Player> receivers = center.getWorld().getNearbyPlayers(center, 48);

            @Override
            public void run() {
                for (int degree = 0; degree < 360; degree += 10) {
                    double addX = r * Math.sin(Math.toRadians(degree));
                    double addY = 0.2;
                    double addZ = r * Math.cos(Math.toRadians(degree));
                    Location newLocation = new Location(center.getWorld(), center.getX() + addX, center.getY() + addY, center.getZ() + addZ);
                    if (r < getRadius()) {
                        Particle.DUST.builder()
                                .data(new Particle.DustOptions(org.bukkit.Color.fromRGB(255, Math.max(255 - colorIncrement * count, 0), Math.max(255 - colorIncrement * count, 0)), 1.5f))
                                .location(newLocation)
                                .receivers(receivers)
                                .spawn();
                    } else {
                        Particle.HEART.builder()
                                .location(newLocation)
                                .receivers(receivers)
                                .spawn();
                        this.cancel();
                    }
                }
                r += rIncrement;
                count++;
            }
        }.runTaskTimer(champions, 0, 1);


    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 10.0, Double.class);
        radius = getConfig("radius", 6.0, Double.class);
        healthBoostStrength = getConfig("healthBoostStrength", 1, Integer.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
