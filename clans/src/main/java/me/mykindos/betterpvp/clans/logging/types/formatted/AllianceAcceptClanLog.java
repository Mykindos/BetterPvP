package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AllianceAcceptClanLog extends FormattedClanLog {
    public AllianceAcceptClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable UUID clan1, @Nullable String clan1Name, @Nullable UUID clan2, @Nullable String clan2Name) {
        super(time, offlinePlayer1, clan1, clan1Name, null, clan2, clan2Name, ClanLogType.CLAN_ALLIANCE_ACCEPT);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("accepted", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("alliance", ClanRelation.ALLY.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(clan2, clan2Name, ClanRelation.ALLY));
    }
}
