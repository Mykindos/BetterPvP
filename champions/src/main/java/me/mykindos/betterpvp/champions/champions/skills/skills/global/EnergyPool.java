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
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.UpdateMaxEnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
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
    public String[] getDescription(int level) {

        return new String[] {
                "Increase your energy pool",
                "by " + getValueString(this::getEnergyIncrease, level),
                "",
                "Energy Information:",
                "<white>Always Active</white>",
                "Base Energy: <stat>" + EnergyService.BASE_ENERGY,
                //Energy is updated every 50ms. Energy is represented in 0-1
                "Energy Regeneration / Second: <stat>" + (EnergyService.BASE_ENERGY_REGEN),
                //"regeneration / second while sprinting ",
                //"or in liquid: <stat>" + (EnergyService.NERFED_ENERGY_REGEN)

        };
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
        baseEnergyPoolIncrease = getConfig("baseEnergyPoolIncrease", 20.0, Double.class);
        energyPoolIncreasePerLevel = getConfig("energyPoolIncreasePerLevel", 10.0, Double.class);
    }

}
