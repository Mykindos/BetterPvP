package me.mykindos.betterpvp.clans.logging.types.formatted;

import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AllianceAcceptClanLog extends FormattedClanLog {
    public AllianceAcceptClanLog(long time, String mainPlayerName, @Nullable UUID mainClan, @Nullable String mainClanName, @Nullable UUID otherClan, @Nullable String otherClanName) {
        super(time, mainPlayerName, mainClan, mainClanName, null, otherClan, otherClanName, ClanLogType.CLAN_ALLIANCE_ACCEPT);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent()
                .append(getPlayerClan1(ClanRelation.SELF)).appendSpace()
                .append(Component.text("accepted", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(Component.text("alliance", ClanRelation.ALLY.getSecondary())).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getClanComponent(otherClan, otherClanName, ClanRelation.ALLY));
    }
}
