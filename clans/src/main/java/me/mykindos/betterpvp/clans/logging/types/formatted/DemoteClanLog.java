package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DemoteClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param offlinePlayer2 player2 of the log
     * @param clan2          clan2 of the log
     */
    public DemoteClanLog(long time, @Nullable OfflinePlayer offlinePlayer1, @Nullable UUID clan1, @Nullable String clan1Name, @Nullable OfflinePlayer offlinePlayer2, @Nullable UUID clan2, @Nullable String clan2Name) {
        super(time, offlinePlayer1, clan1, clan1Name, offlinePlayer2, clan2, clan2Name, ClanLogType.CLAN_DEMOTE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan2(ClanRelation.SELF)).appendSpace()
                .append(Component.text("demoted", NamedTextColor.RED)).appendSpace()
                .append(getPlayerClan1(ClanRelation.SELF));
    }
}
