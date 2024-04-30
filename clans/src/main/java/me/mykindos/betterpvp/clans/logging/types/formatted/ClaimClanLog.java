package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ClaimClanLog extends FormattedClanLog {
    public ClaimClanLog(long time, String mainPlayerName, UUID mainClan, String mainClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, null, null, ClanLogType.CLAN_CLAIM);
    }

    @Override
    public Component getComponent() {
        assert mainPlayerName != null;
        assert mainClan != null;
        return getTimeComponent().append(getPlayerComponent(mainPlayerName, ClanRelation.SELF)).appendSpace()
                .append(Component.text("claimed territory", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("for")).appendSpace()
                .append(getClanComponent(mainClan, mainClanName, ClanRelation.SELF));
    }
}
