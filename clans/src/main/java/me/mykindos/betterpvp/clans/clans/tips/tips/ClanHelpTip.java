package me.mykindos.betterpvp.clans.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.Tip;
import me.mykindos.betterpvp.clans.clans.tips.types.IRunCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanHelpTip extends Tip implements IRunCommand {

    ClanHelpTip() {
        super(1, 1);

    }

    @Override
    public String getName() {
        return "clanhelp";
    }

    private Component generateComponent() {
        Component runComponent = runCommand("/c help");
        Component component = Component.text("You can see a list of commands by running ", NamedTextColor.GRAY).append(runComponent);
        return component;
    }

}
