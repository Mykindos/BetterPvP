package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class CripplingBlow extends Skill implements PassiveSkill, DebuffSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int slownessStrength;

    @Inject
    public CripplingBlow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Crippling Blow";
    }

    @Override
    public Component[] getDescription(int level) {
        Component slowness = Component.text(
                UtilFormat.getRomanNumeral(slownessStrength),
                NamedTextColor.YELLOW
        );
        Component duration = getValueComponent(this::getDuration, level);
        return Translations.componentLines(
                "champions.skill.brute.crippling-blow.description",
                slowness,
                duration
        );
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }


    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!event.isDamageeLiving()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!SkillWeapons.isHolding(player, SkillType.AXE)) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity target = event.getLivingDamagee();
            championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, slownessStrength, (long) (getDuration(level) * 1000));
            event.addReason(getName());
            event.setKnockback(false);
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
    }

}
