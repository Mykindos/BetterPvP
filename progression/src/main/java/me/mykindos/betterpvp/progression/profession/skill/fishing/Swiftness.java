package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class Swiftness extends FishingProgressionSkill implements Listener {

    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public Swiftness(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }


    @Override
    public String getName() {
        return "Swiftness";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Increases catch speed by <green>" + UtilFormat.formatNumber(getSpeedBonus(level), 2) + "%"
        };
    }

    private double getSpeedBonus(int level) {
        return 0.2 * Math.max(1, level);
    }

    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                final int wait = (int) (event.getHook().getWaitTime() * (1 - getSpeedBonus(skillLevel) / 100));
                event.getHook().setWaitTime(Math.max(0, wait));
            }
        });

    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Base Fishing", "No More Mobs"};
        return new ProgressionSkillDependency(dependencies, 1);
    }

    @Override
    public Material getIcon() {
        return Material.COD;
    }

}
