package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class SmokeArrow extends PrepareArrowSkill {

    private double baseDuration;

    @Inject
    public SmokeArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Smoke Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Your next arrow will give <effect>Blindness</effect>",
                "and <effect>Slowness II</effect> to the target for <val>" + getEffectDuration(level) + "</val> seconds.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    private double getEffectDuration(int level) {
        return baseDuration + level;
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
    public double getCooldown(int level) {
        return cooldown - (2 * level);
    }

    @Override
    public void activate(Player player, int level) {
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
    public void onHit(Player damager, LivingEntity target, int level) {
        final int effectDuration = (int) (getEffectDuration(level) * 20L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, effectDuration, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, effectDuration, 1));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);

        UtilMessage.simpleMessage(target, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", damager.getName(), getName());
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s</alt>.", target.getName(), getName());
    }

    @Override
    public void onFire(Player shooter) {
        UtilMessage.message(shooter, getClassType().getName(), "You fired <alt>" + getName() + "<alt>.");
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.SMOKE_LARGE).extra(0).location(location).receivers(60, true).spawn();
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
    }
}
