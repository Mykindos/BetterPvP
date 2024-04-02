package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;

public class UnclaimClanLog extends FormattedClanLog {
    public UnclaimClanLog(long time, OfflinePlayer offlinePlayer1, IOldClan clan1, IOldClan clan2) {
        super(time, offlinePlayer1, clan1, null, clan2, ClanLogType.CLAN_UNCLAIM);
    }

    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text("unclaimed", NamedTextColor.DARK_RED)).appendSpace()
                .append(Component.text("territory", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("from")).appendSpace()
                .append(getClanComponent(clan2, ClanRelation.NEUTRAL));
    }
}
