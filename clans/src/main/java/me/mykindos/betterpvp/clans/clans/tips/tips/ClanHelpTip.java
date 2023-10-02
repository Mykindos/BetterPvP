package me.mykindos.betterpvp.clans.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.ClanTip;
import me.mykindos.betterpvp.clans.clans.tips.types.IRunCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanHelpTip extends ClanTip implements IRunCommand {

    public ClanHelpTip() {
        super(1, 1);
        setComponent(generateComponent());

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

    public boolean isValid(Player player, Clan clan) {
        return true;
    }

}
