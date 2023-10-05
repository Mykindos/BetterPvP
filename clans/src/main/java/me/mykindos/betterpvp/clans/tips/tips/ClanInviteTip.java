package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.clans.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanInviteTip extends ClanTip implements ISuggestCommand {

    public ClanInviteTip() {
        super(2, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "claninvite";
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/c invite <player>", "/c invite ");
        return Component.text("You can invite a player to your clan by running ", NamedTextColor.GRAY).append(suggestComponent);
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getAdminsAsPlayers().contains(player);
    }
}
