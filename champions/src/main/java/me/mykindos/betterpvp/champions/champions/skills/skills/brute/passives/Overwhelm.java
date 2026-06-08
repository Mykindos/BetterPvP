package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Overwhelm extends Skill implements PassiveSkill, DamageSkill {

    private double bonusDamage;
    private double bonusDamageIncreasePerLevel;

    private double healthOverTarget;

    private double baseMaxDamage;
    private double maxDamageIncreasePerLevel;

    @Inject
    public Overwhelm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overwhelm";
    }

    @Override
    public Component[] getDescription(int level) {
        Component health = getValueComponent(this::getHealthOverTarget, level, 100, 0, "%");
        Component bonus = getValueComponent(this::getBonusDamage, level);
        Component max = getValueComponent(this::getMaxDamage, level);
        return Translations.componentLines(
                "champions.skill.brute.overwhelm.description",
                health,
                bonus,
                max
        );
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level-1) * maxDamageIncreasePerLevel);
    }

    public double getBonusDamage(int level) {
        return bonusDamage + bonusDamageIncreasePerLevel * (level - 1);
    }

    public double getHealthOverTarget(int level) {
        return healthOverTarget;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(DamageEvent event) {
        if (!event.isDamageeLiving()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getLivingDamagee() == null) return;
        int level = getLevel(player);
        if (level > 0) {
            final LivingEntity ent = event.getLivingDamagee();
            final double livingEntityHealthPercent = UtilPlayer.getHealthPercentage(ent);
            final double playerHealthPercent = UtilPlayer.getHealthPercentage(player);
            double difference = playerHealthPercent - livingEntityHealthPercent;
            if (difference > 0) {
                // Calculate damage first, THEN cap it
                double damageToAdd = difference * getBonusDamage(level);
                damageToAdd = Math.min(damageToAdd, getMaxDamage(level));

                // Add a flat damage modifier based on health difference
                event.addModifier(new SkillDamageModifier.Flat(this, damageToAdd));
            }
        }
    }

    @Override
    public void loadSkillConfig(){
        bonusDamage = getConfig("bonusDamage", 1.0, Double.class);
        bonusDamageIncreasePerLevel = getConfig("bonusDamageIncreasePerLevel", 0.0, Double.class);
        healthOverTarget = getConfig("healthOverTarget", 0.10, Double.class);
        baseMaxDamage = getConfig("baseMaxDamage", 2.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 1.0, Double.class);
    }


}
