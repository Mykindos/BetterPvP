package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ThickerLines extends FishingProgressionSkill implements Listener {

    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public ThickerLines(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }


    @Override
    public String getName() {
        return "Thicker Lines";
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

    @EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        if(!(event.getLoot() instanceof Fish fish)) return;
        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if(profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
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
