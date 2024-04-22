package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class KickClanLog extends FormattedClanLog {
    public KickClanLog(long time, String mainPlayerName, UUID mainClan, String mainClanName, String otherPlayerName) {
        super(time, mainPlayerName, mainClan, mainClanName, otherPlayerName, null, null, ClanLogType.CLAN_KICK);
    }

    @Override
    public Component getComponent() {
        assert mainPlayerName != null;
        assert mainClan != null;
        assert otherPlayerName != null;
        return getTimeComponent()
                .append(getPlayerComponent(mainPlayerName, ClanRelation.NEUTRAL)).appendSpace()
                .append(Component.text("was")).appendSpace()
                .append(Component.text("kicked", NamedTextColor.RED)).appendSpace()
                .append(Component.text("by")).appendSpace()
                .append(getPlayerComponent(otherPlayerName, ClanRelation.SELF)).appendSpace()
                .append(Component.text("from")).appendSpace()
                .append(getClanComponent(mainClan, mainClanName, ClanRelation.SELF));
    }
}
