package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ViperStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int poisonStrength;

    @Inject
    public ViperStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Viper Strikes";
    }

    @Override
    public Component[] getDescription(int level) {
        Component poison = Translations.component("champions.skill.effect.poison",
                Component.text(UtilFormat.getRomanNumeral(poisonStrength))).color(NamedTextColor.WHITE);
        Component duration = getValueComponent(this::getDuration, level);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.viper-strikes.description",
                poison,
                duration
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

    private double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 2.0, Double.class);
        poisonStrength = getConfig("poisonStrength", 1, Integer.class);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.isCancelled() || !event.isDamageeLiving()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        championsManager.getEffects().addEffect(event.getLivingDamagee(), damager, EffectTypes.POISON, poisonStrength, (long) (getDuration(level) * 1000L));
        event.getDamagee().getWorld().playSound(damager.getLocation(), Sound.ENTITY_SPIDER_HURT, 1f, 2f);
        event.addReason(getName());
    }

}
