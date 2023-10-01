package me.mykindos.betterpvp.clans.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.types.IRunCommand;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanHomeTip extends Tip implements IRunCommand {

    ClanHomeTip() {
        super(2, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanhome";
    }

    private Component generateComponent() {
        Component runComponent = runCommand("/clan sethome");
        Component component = Component.text("You can set a place you can teleport back to in your territory by running ", NamedTextColor.GRAY).append(runComponent);
        return component;
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getAdminsAsPlayers().contains(player);
    }
}
