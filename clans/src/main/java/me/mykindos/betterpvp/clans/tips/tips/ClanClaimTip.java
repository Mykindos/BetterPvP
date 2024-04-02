package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@CustomLog
@Singleton
public class ClanClaimTip extends ClanTip implements ISuggestCommand {

    private final ClanManager clanManager;

    @Inject
    public ClanClaimTip(Clans clans, ClanManager clanManager) {
        super(clans, 1, 2);
        this.clanManager = clanManager;
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanclaim";
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/c claim", "/c claim");
        return Component.text("You can claim territory by running ", NamedTextColor.GRAY).append(suggestComponent);
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        if (clan == null) {
            return false;
        }
        return clan.getAdminsAsPlayers().contains(player) && (clan.getTerritory().size() < clanManager.getMaximumClaimsForClan(clan));
    }
}
