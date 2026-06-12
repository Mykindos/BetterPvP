package me.mykindos.betterpvp.champions.champions.skills.skills.global;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.energy.events.UpdateMaxEnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@Singleton
@BPvPListener
public class EnergyPool extends Skill implements PassiveSkill, BuffSkill {

    private double baseEnergyPoolIncrease;
    private double energyPoolIncreasePerLevel;

    @Inject
    public EnergyPool(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Energy Pool";
    }

    @Override
    public Component[] getDescription(int level) {
        Component incComp = getValueComponent(this::getEnergyIncrease, level);
        Component baseComp = Component.text(UtilFormat.formatNumber(this.championsManager.getEnergy().getMaxEnergy(), 0, true), NamedTextColor.YELLOW);
        Component regenComp = Component.text(UtilFormat.formatNumber(this.championsManager.getEnergy().getEnergyPerSecond(), 1, true), NamedTextColor.YELLOW);

        return Translations.componentLines(
                "champions.skill.global.energy-pool.description",
                incComp,
                baseComp,
                regenComp
        );
    }

    public double getEnergyIncrease(int level) {
        return baseEnergyPoolIncrease + ((level - 1) * energyPoolIncreasePerLevel);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        championsManager.getEnergy().updateMax(player);
        super.invalidatePlayer(player, gamer);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        championsManager.getEnergy().updateMax(player);
        super.trackPlayer(player, gamer);
    }

    @Override
    public void updatePlayer(Player player, Gamer gamer) {
        championsManager.getEnergy().updateMax(player);
        super.updatePlayer(player, gamer);
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @EventHandler
    public void onUpdateMaxEnergy(UpdateMaxEnergyEvent event) {
        final Player player = event.getPlayer();

        int level = getLevel(player);
        if (level > 0) {
            event.setNewMax(event.getNewMax() + getEnergyIncrease(level));
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @Override
    public void loadSkillConfig(){
        baseEnergyPoolIncrease = getConfig("baseEnergyPoolIncrease", 30.0, Double.class);
        energyPoolIncreasePerLevel = getConfig("energyPoolIncreasePerLevel", 15.0, Double.class);
    }

}
