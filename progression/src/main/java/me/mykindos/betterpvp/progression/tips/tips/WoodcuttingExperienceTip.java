package me.mykindos.betterpvp.progression.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.tips.types.IRunCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.tips.ProgressionTip;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class WoodcuttingExperienceTip extends ProgressionTip implements IRunCommand {

    @Inject
    public WoodcuttingExperienceTip(Progression progression) {
        super(progression, 1, 1);
        setComponent(getComponent());
    }

    @Override
    public Component getComponent() {
        Component runComponent = runCommand("/woodcutting");
        return UtilMessage.deserialize("By cutting down trees, you earn woodcutting experience. As you gain experience " +
                "you will level up. Everytime you level up, you will be able to allocate skill points in ").append(runComponent);
    }

    @Override
    public String getName() {
        return "woodcuttingexperiencetip";
    }

    @Override
    public boolean isValid(Player player, ProfessionProfile professionProfile) {
        return true;
    }
}
