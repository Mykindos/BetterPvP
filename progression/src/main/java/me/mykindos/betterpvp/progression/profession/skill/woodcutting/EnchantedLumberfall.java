package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class EnchantedLumberfall extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public EnchantedLumberfall(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Enchanted Lumberfall";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Whenever you fell a tree, that tree will have ___% chance to drop a special item"
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
    }

    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        event.getPlayer().sendMessage("Hi from " + getName());
    }
}
