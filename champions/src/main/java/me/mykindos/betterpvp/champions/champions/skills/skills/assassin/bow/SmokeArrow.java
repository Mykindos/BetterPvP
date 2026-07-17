package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
public class SmokeArrow extends PrepareArrowSkill implements DebuffSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int slownessStrength;

    @Inject
    public SmokeArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Smoke Arrow";
    }

    @Override
    public Component[] getDescription(int level) {
        Component slowness = Component.text(
                UtilFormat.getRomanNumeral(slownessStrength),
                NamedTextColor.YELLOW
        );
        Component duration = getValueComponent(this::getEffectDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines(
                "champions.skill.assassin.smoke-arrow.description",
                slowness,
                duration,
                cooldown
        );
    }

    private double getEffectDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean activate(Player player, int level) {
        if (!active.contains(player.getUniqueId())) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
            active.add(player.getUniqueId());
            return true;
        }
        return false;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        final int effectDuration = (int) (getEffectDuration(level) * 1000L);
        championsManager.getEffects().addEffect(target, damager, EffectTypes.BLINDNESS, 1, effectDuration);
        championsManager.getEffects().addEffect(target, damager, EffectTypes.SLOWNESS, slownessStrength, effectDuration);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);

        new ParticleBuilder(Particle.EXPLOSION)
                .location(target.getLocation())
                .count(1)
                .receivers(60)
                .spawn();

        UtilMessage.message(target, getClassType().getDisplayName(), "champions.skill.hit-by", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(damager, target), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(target, damager), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
    }

    @Override
    public void onFire(Player shooter) {
        UtilMessage.message(shooter, getClassType().getDisplayName(), "champions.skill.assassin.smoke-arrow.fired", getDisplayName().color(NamedTextColor.GREEN));
    }

    @Override
    public void displayTrail(Location location) {
        Random random = UtilMath.RANDOM;
        double spread = 0.1;
        double dx = (random.nextDouble() - 0.5) * spread;
        double dy = (random.nextDouble() - 0.5) * spread;
        double dz = (random.nextDouble() - 0.5) * spread;

        Location particleLocation = location.clone().add(dx, dy, dz);

        double red = 0.2;
        double green = 0.2;
        double blue = 0.2;

        new ParticleBuilder(Particle.ENTITY_EFFECT)
                .location(particleLocation)
                .count(0)
                .offset(red, green, blue)
                .extra(1.0)
                .receivers(60)
                .data(Color.GRAY)
                .spawn();
    }


    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
    }
}
