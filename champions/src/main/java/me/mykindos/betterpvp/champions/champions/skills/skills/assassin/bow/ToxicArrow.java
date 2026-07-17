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
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private double baseDuration;

    private double durationIncreasePerLevel;

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
    public Component[] getDescription(int level) {
        Component poison = Translations.component("champions.skill.effect.poison",
                Component.text(UtilFormat.getRomanNumeral(poisonStrength))).color(NamedTextColor.WHITE);
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.toxic-arrow.description",
                poison,
                duration,
                cooldown
        );
        Component poisonDetail = Translations.component("champions.skill.effect.poison",
                Component.text(UtilFormat.getRomanNumeral(poisonStrength))).color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines(
                "champions.skill.effect.poison.detail",
                poisonDetail,
                Component.text(String.valueOf(poisonStrength * 3), NamedTextColor.GREEN),
                Component.text("1.25", NamedTextColor.YELLOW)
        );
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return (baseDuration + (level - 1) * durationIncreasePerLevel);
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
        championsManager.getEffects().addEffect(target, EffectTypes.POISON, poisonStrength, (long) ((baseDuration + level) * 1000L));
        UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(target, damager), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        UtilMessage.message(target, getClassType().getDisplayName(), "champions.skill.hit-by", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(damager, target), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
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


    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        poisonStrength = getConfig("poisonStrength", 2, Integer.class);
    }
}
