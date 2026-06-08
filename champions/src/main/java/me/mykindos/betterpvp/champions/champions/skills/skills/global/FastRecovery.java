package me.mykindos.betterpvp.champions.champions.skills.skills.global;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@Singleton
@BPvPListener
public class FastRecovery extends Skill implements PassiveSkill, BuffSkill {

    private double basePercentage;
    private double percentagePerLevel;

    @Inject
    public FastRecovery(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fast Recovery";
    }

    @Override
    public Component[] getDescription(int level) {
        Component percentage = getValueComponent(this::getPercentage, level, 100, 0, "%");
        Component baseEnergy = Component.text(
                String.valueOf(this.championsManager.getEnergy().getMaxEnergy()),
                NamedTextColor.YELLOW
        );
        Component energyPerSecond = Component.text(
                String.valueOf(this.championsManager.getEnergy().getEnergyPerSecond()),
                NamedTextColor.YELLOW
        );
        Component alwaysActive = Translations.component("champions.skill.global.always-active").color(NamedTextColor.WHITE);
        return Translations.componentLines(
                "champions.skill.global.fast-recovery.description",
                percentage,
                baseEnergy,
                energyPerSecond,
                alwaysActive
        );
    }

    public double getPercentage(int level) {
        return basePercentage + ((level - 1) * percentagePerLevel);
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @EventHandler
    public void onEnergyRegen(RegenerateEnergyEvent event) {
        Player player = event.getPlayer();

        int level = getLevel(player);
        if (level > 0) {
            event.setEnergy(event.getEnergy() * (1.0 + getPercentage(level)));
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @Override
    public boolean enabledInSpectator() {
        return true;
    }

    @Override
    public void loadSkillConfig(){
        basePercentage = getConfig("basePercentage", 0.10, Double.class);
        percentagePerLevel = getConfig("percentagePerLevel", 0.05, Double.class);
    }

}
