package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class AllianceAcceptClanLog extends FormattedClanLog {
    public AllianceAcceptClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable IOldClan clan1, @Nullable IOldClan clan2) {
        super(time, offlinePlayer1, clan1, null, clan2, ClanLogType.CLAN_ALLIANCE_ACCEPT);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("accepted", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("alliance", ClanRelation.ALLY.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(clan2, ClanRelation.ALLY));
    }
}
