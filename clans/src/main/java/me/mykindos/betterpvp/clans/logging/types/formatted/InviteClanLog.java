package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;
public class InviteClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param player1Name player1 of the log
     * @param clan1          clan1 of the log
     * @param player2Name player2 of the log
     */
    public InviteClanLog(long time, String mainPlayerName, UUID mainClan, String mainClanName, String otherPlayerName) {
        super(time, mainPlayerName, mainClan, mainClanName, otherPlayerName, null, null, ClanLogType.CLAN_INVITE);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("invited", NamedTextColor.GREEN)).appendSpace()
                .append(getPlayerComponent(otherPlayerName, ClanRelation.NEUTRAL));
    }
}
