package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class TreeTactician extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;
    private double xpBonusPerLvl;

    @Inject
    public TreeTactician(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Tree Tactician";
    }


    @Override
    public String[] getDescription(int level) {
        double numberInPercentage = getExperienceBonusForDescription(level) * 100;
        return new String[] {
                "Increases experience gained per chopped log by <green>" + numberInPercentage + "%"
        };
    }

    @Override
    public Material getIcon() {
        return Material.EXPERIENCE_BOTTLE;
    }

    public double getExperienceBonusForDescription(int level) {
        return level*xpBonusPerLvl;
    }

    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;

            event.setExperienceBonusModifier(1.0 + getExperienceBonusForDescription(skillLevel));
        });
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        xpBonusPerLvl = getConfig("xpBonusPerLvl", 0.03, Double.class);
    }
}
