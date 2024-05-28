package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ExpertBaiter extends FishingProgressionSkill implements Listener {

    private final ProfessionProfileManager professionProfileManager;

    @Inject
    protected ExpertBaiter(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Expert Baiter";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Baits you throw last <green>" + UtilFormat.formatNumber(getBonusDuration(level), 2) + "% <gray>longer."
        };
    }

    private double getBonusDuration(int level) {
        return 0.3 * Math.max(1, level);
    }

    @EventHandler
    public void onThrowBait(PlayerThrowBaitEvent event) {
        Player player = event.getPlayer();


        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                event.getBait().setDurationTicks((long) (event.getBait().getDurationTicks() * (1 + getBonusDuration(skillLevel) / 100)));
            }
        });

    }


    @Override
    public Material getIcon() {
        return Material.BREAD;
    }

}
