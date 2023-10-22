package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.clans.tips.types.IRunCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanHelpTip extends ClanTip implements IRunCommand {

    @Inject
    public ClanHelpTip(Clans clans) {
        super(clans, 1, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanhelp";
    }

    @Override
    public Component generateComponent() {
        Component runComponent = runCommand("/c help");
        return Component.text("You can see a list of commands by running ", NamedTextColor.GRAY).append(runComponent);
    }

    public boolean isValid(Player player, Clan clan) {
        return true;
    }

}
