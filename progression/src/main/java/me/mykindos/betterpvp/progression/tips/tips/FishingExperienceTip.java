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
public class FishingExperienceTip extends ProgressionTip implements IRunCommand {

    @Inject
    public FishingExperienceTip(Progression progression) {
        super(progression, 1, 1);
        setComponent(getComponent());
    }

    @Override
    public Component getComponent() {
        Component runComponent = runCommand("/fishing");
        return UtilMessage.deserialize("Level up your fishing by catching fish. " +
                "You can spend your skill points in ").append(runComponent);
    }

    @Override
    public String getName() {
        return "fishingexperiencetip";
    }

    @Override
    public boolean isValid(Player player, ProfessionProfile professionProfile) {
        return true;
    }
}
