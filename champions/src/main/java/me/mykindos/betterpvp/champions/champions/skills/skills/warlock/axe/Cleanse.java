package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

@Getter
@Singleton
@BPvPListener
public class Cleanse extends Skill implements InteractSkill, CooldownSkill, Listener, DefensiveSkill, TeamSkill {

    private double duration;

    private double range;
    private double healthReduction;
    private double healthReductionPerPlayerAffected;

    @Inject
    public Cleanse(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cleanse";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Purge all negative effects from you and your allies within <val>" + getRange() + "</val> blocks",
                "",
                "Affected players also receive an immunity against negative",
                "effects for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "Health Sacrifice: <val>" + UtilFormat.formatNumber(getHealthReduction(), 1) + " + <val>" + UtilFormat.formatNumber(getHealthReductionPerPlayerAffected(), 1) + " per player affected",


        };
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
        if (player.getHealth() - getHealthReduction() <= 1) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s<gray>", getName());
            return false;
        }

        return true;
    }


    @Override
    public void activate(Player player) {
        double healthReduction = getHealthReduction();


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.9f);
        championsManager.getEffects().addEffect(player, EffectTypes.IMMUNE, (long) (getDuration() * 1000L));

        for (Player ally : UtilPlayer.getNearbyAllies(player, player.getLocation(), getRange())) {

            if (player.getHealth() - (healthReduction + getHealthReductionPerPlayerAffected()) < 1) {
                break;
            }
            healthReduction += getHealthReductionPerPlayerAffected();

            championsManager.getEffects().addEffect(ally, EffectTypes.IMMUNE, (long) (getDuration() * 1000L));
            UtilMessage.simpleMessage(ally, "Cleanse", "You were cleansed of negative effects by <alt>" + player.getName());
            UtilServer.callEvent(new EffectClearEvent(ally));
        }

        UtilPlayer.slowHealth(champions, player, -healthReduction, 5, false);
        UtilServer.callEvent(new EffectClearEvent(player));

        BloodCircleEffect.runEffect(player.getLocation().add(new Vector(0, 0.1, 0)), getRange(), Color.fromRGB(255, 255, 150), Color.fromRGB(150, 255, 200));
        final Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 48);
        // Create icon
        double div = 0.5;
        double in = 0.25;
        for (int i = 0; i < 4; i++) {
            Location l1 = player.getLocation().add(new Vector(getRange() * div, 0.1, 0).rotateAroundY(Math.toRadians(i * 90)));
            Location l2 = player.getLocation().add(new Vector(getRange() * div * in, 0.1, getRange() * div * in).rotateAroundY(Math.toRadians(i * 90d)));
            Location l3 = player.getLocation().add(new Vector(0, 0.1, getRange() * div).rotateAroundY(Math.toRadians(i * 90)));

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
        healthReduction = getConfig("healthReduction", 2.0, Double.class);

        range = getConfig("range", 5.0, Double.class);

        duration = getConfig("duration", 2.0, Double.class);

        healthReductionPerPlayerAffected = getConfig("healthReductionPerPlayerAffected", 1.0, Double.class);
    }
}
