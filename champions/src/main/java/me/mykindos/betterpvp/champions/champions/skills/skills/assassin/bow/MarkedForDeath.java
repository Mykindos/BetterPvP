package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class MarkedForDeath extends PrepareArrowSkill implements DebuffSkill {

    @Getter
    private double duration;
    private int vulnerabilityStrength;

    @Inject
    public MarkedForDeath(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Marked for Death";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will give players <effect>Vulnerability " + UtilFormat.getRomanNumeral(vulnerabilityStrength) + "</effect>",
                "for <val>" + getDuration() + "</val> seconds,",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.VULNERABILITY.getDescription(vulnerabilityStrength)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public void onHit(Player damager, LivingEntity target) {
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <yellow>%s</yellow> with <green>%s</green>.", target.getName(), getName());
        championsManager.getEffects().addEffect(target, EffectTypes.VULNERABILITY, vulnerabilityStrength, (long) (getDuration() * 1000L));
        if (!(target instanceof Player damagee)) return;
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", damager.getName(), getName());
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.ENTITY_EFFECT)
                .location(location)
                .data(Color.BLACK)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 6.0, Double.class);
        vulnerabilityStrength = getConfig("vulnerabilityStrength", 4, Integer.class);
    }
}
