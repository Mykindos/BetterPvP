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

@Singleton
@BPvPListener
public class SilencingArrow extends PrepareArrowSkill implements DebuffSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    @Inject
    public SilencingArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Silencing Arrow";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.silencing-arrow.description",
                duration,
                cooldown
        );
        Component silence = Translations.component("champions.skill.effect.silence.name").color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines("champions.skill.effect.silence.detail", silence);
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return (baseDuration + ((level - 1) * durationIncreasePerLevel));
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
    public void onHit(Player damager, LivingEntity target, int level) {
        championsManager.getEffects().addEffect(target, EffectTypes.SILENCE, (long) (getDuration(level)) * 1000L);
        if (championsManager.getEffects().hasEffect(target, EffectTypes.IMMUNE)) {
            UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.assassin.silencing-arrow.immune", this.championsManager.getDisplayNameAsComponent(target, damager));
            return;
        }
        UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameAsComponent(target, damager), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        UtilMessage.message(target, getClassType().getDisplayName(), "champions.skill.hit-by", this.championsManager.getDisplayNameAsComponent(damager, target), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.EFFECT)
                .data(new Particle.Spell(Color.fromRGB(255, 255, 255), 1f))
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }


    @Override
    public boolean activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 0.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
    }
}
