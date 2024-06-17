package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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
        return new String[] {
                "Increases experience gained per chopped log by <green>" + UtilFormat.formatNumber(getExperienceBonus(level), 2) + "%"
        };
    }

    @Override
    public Material getIcon() {
        return Material.EXPERIENCE_BOTTLE;
    }

    public double getExperienceBonus(int level) {
        return 1.0 + level*0.05;
    }

    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                // 0.05 will need to be put in config file
                // u can access through woodcuttingHandler
                // woodcuttingHandler will need to be passed to Event or some other way
                event.setExperienceBonusModifier(getExperienceBonus(skillLevel));
            }
        });
    }
}
