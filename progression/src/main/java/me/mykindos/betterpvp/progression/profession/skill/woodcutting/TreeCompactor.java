package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@CustomLog
@Singleton
@BPvPListener
public class TreeCompactor extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public TreeCompactor(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Tree Compactor";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "You gain access to the /treecompact command",
                "",
                "This command lets you turn a stack of logs",
                "into a singular log letting you store your logs easier"
        };
    }

    @Override
    public Material getIcon() {
        return Material.OAK_WOOD;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    // no dependencies yet
}
