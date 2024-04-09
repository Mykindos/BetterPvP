package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class UnclaimClanLog extends FormattedClanLog {
    public UnclaimClanLog(long time, OfflinePlayer offlinePlayer1, UUID clan1, String clan1Name, UUID clan2, String clan2Name) {
        super(time, offlinePlayer1, clan1, clan1Name, null, clan2, clan2Name, ClanLogType.CLAN_UNCLAIM);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text("unclaimed", NamedTextColor.DARK_RED)).appendSpace()
                .append(Component.text("territory", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("from")).appendSpace()
                .append(getClanComponent(clan2, clan2Name, ClanRelation.NEUTRAL));
    }
}
