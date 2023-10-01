package me.mykindos.betterpvp.clans.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.ClanTip;
import me.mykindos.betterpvp.clans.clans.tips.types.ISuggestCommand;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanInviteTip extends ClanTip implements ISuggestCommand {

    ClanInviteTip() {
        super(2, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "claninvite";
    }


    private Component generateComponent() {
        Component suggestComponent = suggestCommand("/c invite <player>", "/c invite ");
        Component component = Component.text("You can invite a player to your clan by running ", NamedTextColor.GRAY).append(suggestComponent);
        return component;
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getAdminsAsPlayers().contains(player);
    }
}
