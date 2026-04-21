package me.mykindos.betterpvp.progression.profession.skill.fishing.expertbaiter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@Singleton
@NodeId("expert_baiter")
public class ExpertBaiter extends ProfessionSkill {

    @Inject
    public ExpertBaiter() {
        super("Expert Baiter");
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Learn to craft baits."
        };
    }

    private double getBonusDuration(int level) {
        return 0.3 * Math.max(1, level);
    }

    public void onThrowBait(PlayerThrowBaitEvent event) {
        Player player = event.getPlayer();


        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = getSkillLevel(player);
                if (skillLevel <= 0) return;

                event.getBait().setDurationInTicks((long) (event.getBait().getDurationInTicks() * (1 + getBonusDuration(skillLevel) / 100)));
            }
        });

    }


    @Override
    public Material getIcon() {
        return Material.BREAD;
    }

}
