
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
public class MarkedForDeath extends PrepareArrowSkill implements DebuffSkill {


    private double baseDuration;

    private double durationIncreasePerLevel;

    private int vulnerabilityStrength;
    private int vulnerabilityIncreasePerLevel;

    @Inject
    public MarkedForDeath(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Marked for Death";
    }

    @Override
    public Component[] getDescription(int level) {
        Component strength = Translations.component("champions.skill.effect.vulnerability",
                Component.text(UtilFormat.getRomanNumeral(getVulnerabilityStrength(level)))).color(NamedTextColor.WHITE);
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.marked-for-death.description",
                strength,
                duration,
                cooldown
        );
        Component vulnerabilityDetail = Translations.component("champions.skill.effect.vulnerability",
                Component.text(UtilFormat.getRomanNumeral(getVulnerabilityStrength(level)))).color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines(
                "champions.skill.effect.vulnerability.detail",
                vulnerabilityDetail,
                Component.text(String.valueOf(getVulnerabilityStrength(level) * 10), NamedTextColor.GREEN)
        );
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return (baseDuration + ((level - 1) * durationIncreasePerLevel));
    }

    public int getVulnerabilityStrength(int level) {
        return vulnerabilityStrength + ((level - 1) * vulnerabilityIncreasePerLevel);
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
    public void onHit(Player damager, LivingEntity target, int level) {
        UtilMessage.message(damager, getClassType().getDisplayName(), "champions.skill.hit-target", Component.text(target.getName(), NamedTextColor.YELLOW), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
        championsManager.getEffects().addEffect(target, EffectTypes.VULNERABILITY, getVulnerabilityStrength(level), (long) (getDuration(level) * 1000L));
        if (!(target instanceof Player damagee)) return;
        UtilMessage.message(damagee, getClassType().getDisplayName(), "champions.skill.hit-by", Component.text(damager.getName(), NamedTextColor.YELLOW), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
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
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 6.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        vulnerabilityStrength = getConfig("vulnerabilityStrength", 2, Integer.class);
        vulnerabilityIncreasePerLevel = getConfig("vulnerabilityIncreasePerLevel", 0, Integer.class);
    }
}
