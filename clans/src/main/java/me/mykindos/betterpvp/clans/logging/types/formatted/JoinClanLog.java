package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class JoinClanLog extends FormattedClanLog {
    public JoinClanLog(long time, String mainPlayerName, UUID mainClan, String mainClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, null, null, ClanLogType.CLAN_JOIN);
    }

    @Override
    public Component getComponent() {
        assert mainPlayerName != null;
        assert mainClan != null;
        return getTimeComponent()
                .append(getPlayerComponent(mainPlayerName, ClanRelation.SELF)).appendSpace()
                .append(Component.text("joined", NamedTextColor.GREEN)).appendSpace()
                .append(getClanComponent(mainClan, mainClanName, ClanRelation.SELF));
    }
}
