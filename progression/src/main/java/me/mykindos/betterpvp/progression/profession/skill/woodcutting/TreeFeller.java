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
public class TreeFeller extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public TreeFeller(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Tree Feller";
    }

    // add cooldown calculation in here!
    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Cut down an entire tree by chopping a single log",
                "Cooldown tbd"
        };
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_AXE;
    }


    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                player.sendMessage("you have tree feller and chopped log");
            }
        });
    }
}
