package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.mykindos.betterpvp.core.locale.Translations;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class LevitatingShot extends PrepareArrowSkill implements OffensiveSkill, DebuffSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int levitationStrength;

    @Inject
    public LevitatingShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Levitating Shot";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component levitationIV = Translations.component("champions.skill.effect.levitation",
                Component.text("IV")).color(NamedTextColor.WHITE);
        Component levitation = Translations.component("champions.skill.effect.levitation.name").color(NamedTextColor.WHITE);
        return Translations.componentLines("champions.skill.ranger.levitating-shot.description", duration, cooldown, levitationIV, levitation);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.BOW;
    }

    @Override
    public boolean activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
        return true;
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {

        championsManager.getEffects().addEffect(target, damager, EffectTypes.LEVITATION, levitationStrength, (int) (getDuration(level) * 1000));
        UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(target, damager), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        UtilMessage.message(target, getClassType().getDisplayName(), "champions.skill.hit-by-alt", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(damager, target), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));

    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.ENCHANT)
                .location(location)
                .count(3)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);

        levitationStrength = getConfig("levitationStrength", 4, Integer.class);
    }
}
