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
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public Component[] getDescription(int level) {
        Component range = getValueComponent(this::getRange, level);
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component healthReduction = getValueComponent(this::getHealthReduction, level);
        Component healthReductionPerPlayer = getValueComponent(this::getHealthReductionPerPlayerAffected, level);
        return Translations.componentLines("champions.skill.warlock.cleanse.description", range, duration, cooldown, healthReduction, healthReductionPerPlayer);
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
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.not-enough-health", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));
            return false;
        }

        return true;
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean activate(Player player, int level) {
        double healthReduction = getHealthReduction(level);


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.9f);
        championsManager.getEffects().addEffect(player, EffectTypes.IMMUNE, (long) (getDuration(level) * 1000L));

        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRange(level))) {

            if(player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected(level)) < 1) {
                break;
            }
            healthReduction += getHealthReductionPerPlayerAffected(level);

            championsManager.getEffects().addEffect(ally, EffectTypes.IMMUNE, (long) (getDuration(level) * 1000L));
            UtilMessage.message(ally, "core.prefix.cleanse", "champions.skill.warlock.cleanse.cleansed", this.championsManager.getDisplayNameAsComponent(player, ally));
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

        return true;
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
        baseHealthReduction = getConfig("baseHealthReduction", 4.0, Double.class);
        healthReductionDecreasePerLevel = getConfig("healthReductionDecreasePerLevel", 0.0, Double.class);

        baseRange = getConfig("baseRange", 5.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 1.0, Double.class);

        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);

        baseHealthReductionPerPlayerAffected = getConfig("baseHealthReductionPerPlayerAffected", 0.0, Double.class);
        healthReductionPerPlayerAffectedDecreasePerLevel = getConfig("healthReductionPerPlayerAffectedDecreasePerLevel", 0.0, Double.class);
    }
}
