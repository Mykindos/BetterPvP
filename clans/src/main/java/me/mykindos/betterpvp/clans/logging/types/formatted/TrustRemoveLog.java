package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TrustRemoveLog extends FormattedClanLog{
    /**
     * @param offlinePlayer2 player2 of the log
     * @param type           the type of log
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     * @param clan1Name
     * @param clan2          clan2 of the log
     * @param clan2Name
     */
    public TrustRemoveLog(long time, String mainPlayerName, @Nullable UUID mainClan, @Nullable String mainClanName, @Nullable UUID otherClan, @Nullable String otherClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, otherClan, otherClanName, ClanLogType.CLAN_TRUST_REMOVE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("removed", NamedTextColor.DARK_RED)).appendSpace()
                .append(Component.text("trust", ClanRelation.ALLY_TRUST.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(otherClan, otherClanName, ClanRelation.ALLY));
    }
}