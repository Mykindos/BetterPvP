package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DemoteClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param player1Name player1 of the log
     * @param clan1          clan1 of the log
     * @param player2Name player2 of the log
     * @param clan2          clan2 of the log
     */
    public DemoteClanLog(long time, @Nullable String mainPlayerName, @Nullable UUID mainClan, @Nullable String mainClanName, @Nullable String otherPlayerName, @Nullable UUID otherClan, @Nullable String otherClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, otherPlayerName, otherClan, otherClanName, ClanLogType.CLAN_DEMOTE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan2(ClanRelation.SELF)).appendSpace()
                .append(Component.text("demoted", NamedTextColor.RED)).appendSpace()
                .append(getPlayerClan1(ClanRelation.SELF));
    }
}
