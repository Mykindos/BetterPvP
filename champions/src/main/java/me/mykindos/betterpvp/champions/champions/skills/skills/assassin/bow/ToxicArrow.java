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
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.Random;

@Singleton
@BPvPListener
public class ToxicArrow extends PrepareArrowSkill implements DebuffSkill {

    @Getter
    private double duration;
    private int poisonStrength;

    @Inject
    public ToxicArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Toxic Arrow";
    }

    @Override
    public String[] getDescription() {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will give your target ",
                "<effect>Poison " + UtilFormat.getRomanNumeral(poisonStrength) + "</effect> for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.POISON.getDescription(poisonStrength)

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
    public void activate(Player player) {
        if (!active.contains(player.getUniqueId())) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
            active.add(player.getUniqueId());
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void onHit(Player damager, LivingEntity target) {
        championsManager.getEffects().addEffect(target, EffectTypes.POISON, poisonStrength, (long) (getDuration() * 1000L));
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <yellow>%s</yellow> with <green>%s</green>.", target.getName(), getName());
        if (!(target instanceof Player damagee)) return;
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", damager.getName(), getName());
    }

    @Override
    public void displayTrail(Location location) {
        Random random = UtilMath.RANDOM;
        double spread = 0.1;
        double dx = (random.nextDouble() - 0.5) * spread;
        double dy = (random.nextDouble() - 0.5) * spread;
        double dz = (random.nextDouble() - 0.5) * spread;

        Location particleLocation = location.clone().add(dx, dy, dz);

        double red = 0.4;
        double green = 1.0;
        double blue = 0.4;

        new ParticleBuilder(Particle.ENTITY_EFFECT)
                .location(particleLocation)
                .count(0)
                .data(Color.GREEN)
                .offset(red, green, blue)
                .extra(1.0)
                .receivers(60)
                .spawn();
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 4.0, Double.class);
        poisonStrength = getConfig("poisonStrength", 2, Integer.class);
    }
}
