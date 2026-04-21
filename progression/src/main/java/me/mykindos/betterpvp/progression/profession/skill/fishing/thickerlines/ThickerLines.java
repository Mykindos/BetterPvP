package me.mykindos.betterpvp.progression.profession.skill.fishing.thickerlines;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@Singleton
@NodeId("thicker_lines")
public class ThickerLines extends ProfessionSkill {

    @Inject
    public ThickerLines() {
        super("Thicker Lines");
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Increases weight of fish caught by <green>" + UtilFormat.formatNumber(getWeightBonus(level), 2) + "%"
        };
    }

    private double getWeightBonus(int level) {
        return 0.08 * Math.max(1, level);
    }

    public void onCatchFish(PlayerCaughtFishEvent event) {
        if(!(event.getLoot() instanceof Fish fish)) return;
        Player player = event.getPlayer();
        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if(profession != null) {
                int skillLevel = getSkillLevel(player);
                if(skillLevel <= 0) return;

                fish.setWeight((int) (fish.getWeight() * (1 + getWeightBonus(skillLevel) / 100)));
            }
        });
    }

    @Override
    public Material getIcon() {
        return Material.STRING;
    }

}
