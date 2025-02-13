package me.mykindos.betterpvp.champions.champions.skills.skills.global;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@Getter
@Singleton
@BPvPListener
public class FastRecovery extends Skill implements PassiveSkill, BuffSkill {

    private double percentage;

    @Inject
    public FastRecovery(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fast Recovery";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Increase your energy regeneration",
                "speed by <val>" + UtilFormat.formatNumber(getPercentage() * 100, 0) + "%</val>",
                "",
                "Energy Information:",
                "<white>Always Active</white>",
                "Base Energy: <stat>" + EnergyHandler.BASE_ENERGY,
                //Energy is updated every 50ms. Energy is represented in 0-1
                "Energy Regeneration / Second: <stat>" + (EnergyHandler.BASE_ENERGY_REGEN * (1000d / EnergyHandler.UPDATE_RATE) * 100),
                //"regeneration / second while sprinting ",
                //"or in liquid: <stat>" + (EnergyHandler.nerfedEnergyRegen * (1000/EnergyHandler.updateRate) * 100)

        };
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @EventHandler
    public void onEnergyRegen(RegenerateEnergyEvent event) {
        Player player = event.getPlayer();

        if (hasSkill(player)) {
            event.setEnergy(event.getEnergy() * (1.0 + getPercentage()));
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @Override
    public void loadSkillConfig() {
        percentage = getConfig("percentage", 0.15, Double.class);
    }

}
