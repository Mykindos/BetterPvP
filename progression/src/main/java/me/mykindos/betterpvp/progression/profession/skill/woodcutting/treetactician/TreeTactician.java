package me.mykindos.betterpvp.progression.profession.skill.woodcutting.treetactician;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@Singleton
@SkillId("tree_tactician")
public class TreeTactician extends ProfessionSkill {

    private double xpBonusPerLvl;

    @Inject
    public TreeTactician() {
        super("Tree Tactician");
    }

    @Override
    public String[] getDescription(int level) {
        double numberInPercentage = getExperienceBonusForDescription(level) * 100;
        String formattedNumber = UtilFormat.formatNumber(numberInPercentage, 2);

        return new String[] {
                "Increases experience gained per chopped log by <green>" + formattedNumber + "%"
        };
    }

    @Override
    public Material getIcon() {
        return Material.EXPERIENCE_BOTTLE;
    }

    public double getExperienceBonusForDescription(int level) {
        return level*xpBonusPerLvl;
    }

    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        Player player = event.getPlayer();
        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getSkillLevel(profile);
            if (skillLevel <= 0) return;

            event.setExperienceBonusModifier(1.0 + getExperienceBonusForDescription(skillLevel));
        });
    }

    @Override
    public void loadSkillConfig() {
        
        xpBonusPerLvl = getSkillConfig("xpBonusPerLvl", 0.04, Double.class);
    }
}
