package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class AllianceRemoveClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param offlinePlayer2 player2 of the log
     * @param clan2          clan2 of the log
     * @param type           the type of log
     */
    public AllianceRemoveClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable IOldClan clan1, @Nullable IOldClan clan2) {
        super(time, offlinePlayer1, clan1, null, clan2, ClanLogType.CLAN_ALLIANCE_REMOVE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("removed", NamedTextColor.DARK_RED)).appendSpace()
                .append(Component.text("alliance", ClanRelation.ALLY.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(clan2, ClanRelation.NEUTRAL));
    }
}
