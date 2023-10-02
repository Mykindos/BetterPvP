package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.clans.tips.types.IRunCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanHomeTip extends ClanTip implements IRunCommand {

    public ClanHomeTip() {
        super(2, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanhome";
    }

    @Override
    public Component generateComponent() {
        Component runComponent = runCommand("/clan sethome");
        return Component.text("You can set a place you can teleport back to in your territory by running ", NamedTextColor.GRAY).append(runComponent);
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getAdminsAsPlayers().contains(player);
    }
}
