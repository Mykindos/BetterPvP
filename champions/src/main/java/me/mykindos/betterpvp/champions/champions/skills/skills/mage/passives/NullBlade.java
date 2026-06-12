package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@Singleton
@BPvPListener
public class NullBlade extends Skill implements PassiveSkill, OffensiveSkill {

    private double energySiphoned;
    private double energySiphonedIncreasePerLevel;

    @Inject
    public NullBlade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Null Blade";
    }

    @Override
    public Component[] getDescription(int level) {
        Component siphonedEnergy = getValueComponent(this::getSiphonedEnergy, level);
        return Translations.componentLines("champions.skill.mage.null-blade.description", siphonedEnergy);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (UtilEntity.isPlayerSpawned(event.getDamagee())) return;

        if (!(event.getDamager() instanceof Player dam)) return;

        int level = getLevel(dam);
        if (level > 0) {
            double degeneration = getSiphonedEnergy(level);

            if (event.getDamagee() instanceof Player target) {
                championsManager.getEnergy().degenerateEnergy(target, degeneration, EnergyEvent.Cause.CUSTOM);
            }
            championsManager.getEnergy().regenerateEnergy(dam, degeneration, EnergyEvent.Cause.CUSTOM);
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    public float getSiphonedEnergy(int level) {
        return (float) (energySiphoned + ((level - 1) * energySiphonedIncreasePerLevel));
    }

    @Override
    public void loadSkillConfig() {
        energySiphoned = getConfig("energySiphoned", 9.0, Double.class);
        energySiphonedIncreasePerLevel = getConfig("energySiphonedIncreasePerLevel", 3.0, Double.class);
    }
}
