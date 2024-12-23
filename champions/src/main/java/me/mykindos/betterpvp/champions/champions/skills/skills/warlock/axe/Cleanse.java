package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe.bloodeffects.BloodCircleEffect;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.Collection;

@Singleton
@BPvPListener
public class Cleanse extends Skill implements InteractSkill, CooldownSkill, Listener, DefensiveSkill, TeamSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double baseRange;
    private double rangeIncreasePerLevel;
    private double baseHealthReduction;
    private double healthReductionDecreasePerLevel;
    private double baseHealthReductionPerPlayerAffected;
    private double healthReductionPerPlayerAffectedDecreasePerLevel;

    @Inject
    public Cleanse(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cleanse";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Purge all negative effects from you and your allies within " + getValueString(this::getRange, level) + " blocks",
                "",
                "Affected players also receive an immunity against negative",
                "effects for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Health Sacrifice: " + getValueString(this::getHealthReduction, level, 1) + " + " + getValueString(this::getHealthReductionPerPlayerAffected, level, 1) + " per player affected",


        };
    }

    public double getHealthReduction(int level) {
        return baseHealthReduction - ((level - 1) * healthReductionDecreasePerLevel);
    }

    public double getRange(int level) {
        return baseRange + ((level - 1) * rangeIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getHealthReductionPerPlayerAffected(int level) {
        return baseHealthReductionPerPlayerAffected - ((level - 1) * healthReductionPerPlayerAffectedDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);

        if (player.getHealth() - getHealthReduction(level) <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        double healthReduction = getHealthReduction(level);


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.9f);
        championsManager.getEffects().addEffect(player, EffectTypes.IMMUNE, (long) (getDuration(level) * 1000L));

        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRange(level))) {

            if(player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected(level)) < 1) {
                break;
            }
            healthReduction += getHealthReductionPerPlayerAffected(level);

            championsManager.getEffects().addEffect(ally, EffectTypes.IMMUNE, (long) (getDuration(level) * 1000L));
            UtilMessage.simpleMessage(ally, "Cleanse", "You were cleansed of negative effects by <alt>" + player.getName());
            UtilServer.callEvent(new EffectClearEvent(ally));
        }

        UtilPlayer.slowHealth(champions, player, -healthReduction, 5, false);
        UtilServer.callEvent(new EffectClearEvent(player));

        BloodCircleEffect.runEffect(player.getLocation().add(new Vector(0, 0.1, 0)), getRange(level), Color.fromRGB(255, 255, 150), Color.fromRGB(150, 255, 200));
        final Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 48);
        // Create icon
        double div = 0.5;
        double in = 0.25;
        for (int i = 0; i < 4; i++) {
            Location l1 = player.getLocation().add(new Vector(getRange(level) * div, 0.1, 0).rotateAroundY(Math.toRadians(i * 90)));
            Location l2 = player.getLocation().add(new Vector(getRange(level) * div * in, 0.1, getRange(level) * div * in).rotateAroundY(Math.toRadians(i * 90d)));
            Location l3 = player.getLocation().add(new Vector(0, 0.1, getRange(level) * div).rotateAroundY(Math.toRadians(i * 90)));

            for (Location l : VectorLine.withStepSize(l1, l2, 0.15d).toLocations()) {
                Particle.END_ROD.builder()
                        .location(l)
                        .receivers(receivers)
                        .extra(0.f)
                        .spawn();
            }
            for (Location l : VectorLine.withStepSize(l2, l3, 0.15d).toLocations()) {
                Particle.END_ROD.builder()
                        .location(l)
                        .receivers(receivers)
                        .extra(0.f)
                        .spawn();
            }
        }

    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean ignoreNegativeEffects() {
        return true;
    }

    @Override
    public void loadSkillConfig() {
        baseHealthReduction = getConfig("baseHealthReduction", 2.0, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.0, Double.class);

        baseRange = getConfig("baseRange", 5.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 1.0, Double.class);

        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);

        baseHealthReductionPerPlayerAffected = getConfig("baseHealthReductionPerPlayerAffected", 1.0, Double.class);
        healthReductionPerPlayerAffectedDecreasePerLevel = getConfig("healthReductionPerPlayerAffectedDecreasePerLevel", 0.0, Double.class);
    }
}
