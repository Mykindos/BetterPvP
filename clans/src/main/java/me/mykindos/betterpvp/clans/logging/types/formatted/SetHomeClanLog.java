package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class SetHomeClanLog extends FormattedClanLog {
    /**
     * @param time           the time this log was generated
     * @param offlinePlayer1 player1 of the log
     * @param clan1          clan1 of the log
     */
    public SetHomeClanLog(long time, String mainPlayerName, UUID mainClan, String mainClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, null, null, ClanLogType.CLAN_CLAIM);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("set", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("the clan home"));
    }
}
